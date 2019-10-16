(ns bs.core
  (:require [bs.board :as board]
            [bs.algorithm :as alg]
            [bs.animation :as animation]
            [bs.utils :as u]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn- cell-id [[x y]]
  (str "cell-" x "-" y))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(defn- new-db []
  (let [[src target] [[10 10] [30 15]]]
    {:db/board (-> (board/make 56 20)
                   (board/set-source src)
                   (board/set-target target))
     :db/current-alg (first alg/ALL)}))

(defn- show-error! [state err]
  (swap! state assoc :db/error err)
  (js/setTimeout #(swap! state dissoc :db/error) 5000))

(defn- process-alg
  "Takes the current algorithm and the current board from the state
  and processes the current algorithm on it."
  [{:db/keys [current-alg board]}]
  (alg/process (::alg/key current-alg) board
               (:board/source board) (:board/target board)))

(defn- update!
  "A middleware like way to update the app-state. If the algorithm or
  the board changes, recalculate the algorithm"
  [state f & args]
  (let [old-state @state
        new-state (apply f @state args)]
    (if (and (contains? old-state :db/alg-result)
             (or (not (identical? (:db/board old-state) (:db/board new-state)))
                 (not (identical? (:db/current-alg old-state) (:db/current-alg new-state)))))
      (reset! state (assoc new-state :db/alg-result (process-alg new-state)))
      (reset! state new-state))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(def SPEED 8)

(defn- animate!* [state {::alg/keys [shortest-path visitation-order] :as alg-result}]
  (let [steps (concat
               (map #(animation/make-step (cell-id %) "cell--visited-animated" SPEED)
                    visitation-order)
               (map #(animation/make-step (cell-id %) "cell--path-animated" (* SPEED 4))
                    shortest-path))
        animation (animation/start! steps #(swap! state assoc
                                                  :db/animation %
                                                  :db/alg-result alg-result))]
    (swap! state assoc :db/animation animation)))

(defn- animate! [state]
  (let [{::alg/keys [shortest-path] :as result} (process-alg @state)]
    (if (empty? shortest-path)
      (show-error! state "Target is unreachable")
      (animate!* state result))))

(defn board-table [state]
  (let [{:db/keys [board alg-result animation] :as st} @state
        {:board/keys [width height] :as board} board
        animating? (animation/running? animation)
        {::alg/keys [shortest-path visitation-order]} (when-not animating? alg-result)
        path? (let [s (set shortest-path)] #(contains? s %2))
        visited? (let [s (set visitation-order)] #(contains? s %2))
        ;; For react performance, don't swap in every wall while
        ;; dragging, but rather natively animate them, store them in a
        ;; cache and flush them to the app db on mouse up.
        new-walls-cache (transient [])

        start-drag! (fn [pos]
                      (let [type (cond (board/source? board pos) :drag/source
                                       (board/target? board pos) :drag/target
                                       :else :drag/wall)]
                        (update! state assoc :db/dragging type)
                        (when (= :drag/wall type)
                          (update! state update :db/board board/make-wall pos))))
        drag-to!     (fn [pos]
                       (when-let [f (case (:db/dragging st)
                                      :drag/source board/set-source
                                      :drag/target board/set-target
                                      nil)]
                         (update! state update :db/board f pos))
                       (when (= :drag/wall (:db/dragging st))
                         (u/add-class! (cell-id pos) "cell--wall-animated")
                         (conj! new-walls-cache pos)))
        end-drag!     (fn []
                        (when (contains? st :db/dragging)
                          (update! state dissoc :db/dragging)
                          (when-let [new-walls (seq (persistent! new-walls-cache))]
                            (update! state assoc :db/board
                                     (reduce board/make-wall board new-walls)))))]
    [:table {:on-mouse-leave end-drag!}
     [:tbody {:class (when animating? "cursor-not-allowed")}
      (for [y (range height)]
        ^{:key y}
        [:tr
         (for [x (range width)
               :let [pos [x y]]]
           ^{:key x}
           [:td.cell
            {:id (cell-id pos)
             :class (for [[f v] {board/source? "cell--source-animated"
                                 board/target? "cell--target-animated"
                                 board/wall? "cell--wall"
                                 path? "cell--path"
                                 visited? "cell--visited"}
                          :when (f board [x y])] v)
             :on-mouse-down (when-not animating? #(start-drag! pos))
             :on-mouse-enter #(drag-to! pos)
             :on-mouse-up end-drag!}])])]]))

(defn- algo-dropdown []
  (let [open? (r/atom false)]
    (fn [{:keys [current on-change]}]
      [:div
       [:button.dropdown__select
        {:on-click #(reset! open? true)}
        (::alg/name current)
        [:i.mdi.mdi-chevron-down]]
       (when @open?
         [:<>
          [:div.fixed.cursor-default.inset-0 {:on-click #(reset! open? false)}]
          [:ul.dropdown__options
           (for [{::alg/keys [key name] :as alg} alg/ALL]
             ^{:key key}
             [:li.dropdown__option
              {:on-click (fn [] (reset! open? false) (on-change alg))}
              (when (= current alg) [:i.mdi.mdi-graph-outline.mr-3])
              [:span.leading-loose name]])]])])))

(defn root [state]
  [:<>
   [:div.header
    [:span.logo-title.mr-2]
    [:h1.text-3xl.inline-block.text-white.mr-8 "Boyscout"]
    [algo-dropdown {:on-change #(update! state assoc :db/current-alg %)
                    :current (:db/current-alg @state)}]
    (if (animation/running? (:db/animation @state))
      [:button.btn.btn--red
       {:on-click #(swap! state update :db/animation animation/cancel!)}
       [:i.mdi.mdi-stop-circle-outline.animate-pulsing]
       [:span.pl-3.font-bold.text-base "Stop"]]
      [:button.btn.btn--header {:on-click #(animate! state)} "Visualize!"])
    [:button.text-white.underline.hover:no-underline.ml-auto
     {:on-click #(reset! state (new-db))} "Reset"]]
   (when-let [e (:db/error @state)]
     [:div.alert [:p e]])
   [:div.container.mx-auto.tracking-wide.mt-8
    [board-table state]]])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
