(ns bs.core
  (:require [bs.algorithm :as alg]
            [bs.board :as board]
            [bs.db :as db]
            [bs.views :as views]
            [bs.utils :as u]
            [reagent.core :as r]))

(defn board-table [state]
  (let [{:db/keys [board alg-result] :as st} @state
        {:board/keys [width height] :as board} board
        animating? (db/animating? st)
        {::alg/keys [path visitation-order]} (when-not animating? alg-result)
        path? (let [s (set path)] #(contains? s %2))
        visited? (let [s (set visitation-order)] #(contains? s %2))
        ;; For react performance, don't swap in every wall while
        ;; dragging, but rather natively animate them, store them in a
        ;; cache and flush them to the app db on mouse up.
        new-walls-cache (transient [])

        start-drag! (fn [pos]
                      (let [type (cond (board/source? board pos) :drag/source
                                       (board/target? board pos) :drag/target
                                       :else :drag/wall)]
                        (db/update! state assoc :db/dragging type)
                        (when (= :drag/wall type)
                          (db/update! state update :db/board board/make-wall pos))))
        drag-to!     (fn [pos]
                       (when-let [f (case (:db/dragging st)
                                      :drag/source board/set-source
                                      :drag/target board/set-target
                                      nil)]
                         (db/update! state update :db/board f pos))
                       (when (= :drag/wall (:db/dragging st))
                         (u/add-class! (board/cell-id pos) "cell--wall-animated")
                         (conj! new-walls-cache pos)))
        end-drag!     (fn []
                        (when (contains? st :db/dragging)
                          (db/update! state dissoc :db/dragging)
                          (when-let [new-walls (seq (persistent! new-walls-cache))]
                            (db/update! state update :db/board board/make-walls new-walls))))]
    [:table {:on-mouse-leave end-drag!}
     [:tbody {:class (when animating? "cursor-not-allowed")}
      (for [y (range height)]
        ^{:key y}
        [:tr
         (for [x (range width)
               :let [pos [x y]]]
           ^{:key x}
           [:td.cell
            {:id (board/cell-id pos)
             :class (for [[f v] {board/source? "cell--source"
                                 board/target? "cell--target"
                                 board/wall? "cell--wall"
                                 path? "cell--path"
                                 visited? "cell--visited"}
                          :when (f board [x y])] v)
             :on-mouse-down (when-not animating? #(start-drag! pos))
             :on-mouse-enter #(drag-to! pos)
             :on-mouse-up end-drag!}])])]]))

(defn root [state]
  (let [animating? (db/animating? @state)]
    [:<>
     [:div.header
      [:span.logo-title.mr-2]
      [:h1.text-3xl.inline-block.text-white.mr-8 "Boyscout"]
      (if animating?
        [:button.btn.btn--red.mx-4
         {:on-click #(db/cancel-animation! state)}
         [:i.mdi.mdi-stop-circle-outline.animate-pulsing]
         [:span.pl-3.font-bold.text-base "Stop"]]
        [:button.btn.mx-4.text-white.hover:bg-white.hover:text-blue-700
         {:on-click #(db/animate! state)} "Visualize!"])
      (when-not animating?
        [:button.mx-4.text-white.underline.hover:no-underline
         {:on-click #(reset! state (db/new-db))} "Reset"])]
     (when-let [e (:db/error @state)]
       [:div.alert [:p e]])
     [:div.flex.mt-8.flex-wrap
      [:div.px-8 {:class "xl:w-1/4"}
       [views/algorithm-summary state]]
      [:div.px-3.mt-8.lg:mt-0 {:class "xl:w-3/4"}
       [board-table state]]]]))

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (db/new-db))] (.getElementById js/document "app")))

(def main! render!)
