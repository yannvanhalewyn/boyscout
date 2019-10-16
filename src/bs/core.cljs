(ns bs.core
  (:require [bs.board :as board]
            [bs.algorithm :as alg]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn- add-class! [id class]
  (.add (.-classList (.getElementById js/document id)) class))

(defn- cell-id [[x y]]
  (str "cell-" x "-" y))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(defn- new-db []
  (let [[src target] [[10 10] [30 15]]]
    {:db/board (-> (board/make 66 20)
                   (board/set-source src)
                   (board/set-target target))
     :db/current-alg (first alg/ALL)}))

(defn- show-error! [state err]
  (swap! state assoc :db/error err)
  (js/setTimeout #(swap! state dissoc :db/error) 2000))

(defn- current-algorithm-results [{:db/keys [board current-alg]}]
  (alg/process (::alg/key current-alg) board
               (:board/source board) (:board/target board)))

(defn- update-board-from-algorithm [board {::alg/keys [shortest-path visitation-order]}]
  (board/set-path-and-visited board shortest-path visitation-order))

(defn- update-board!
  "Swaps the state with a new board. When it already has been
  animated, will recalculate the algorithm result."
  [state new-board]
  (if (seq (:board/path new-board))
    (let [result (current-algorithm-results (assoc @state :db/board new-board))]
      (swap! state assoc :db/board (update-board-from-algorithm new-board result)))
    (swap! state assoc :db/board new-board)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(def SPEED 8)

(defn- animate!* [state {::alg/keys [shortest-path visitation-order] :as alg-result}]
  (swap! state update :db/board board/set-path-and-visited nil nil)
  (doseq [[i pos] (map-indexed vector visitation-order)]
    (js/setTimeout
     #(add-class! (cell-id pos) "cell--visited-animated")
     (* i SPEED)))
  (doseq [[i pos] (map-indexed vector shortest-path)]
    (js/setTimeout
     #(add-class! (cell-id pos) "cell--path-animated")
     (+ (* (count visitation-order) SPEED) (* i SPEED 4))))
  (js/setTimeout
   #(swap! state update :db/board update-board-from-algorithm alg-result)
   (+ (* (count visitation-order) SPEED)
      (* (count shortest-path) SPEED 4))))

(defn- animate! [state]
  (let [{::alg/keys [shortest-path] :as result} (current-algorithm-results @state)]
    (if (empty? shortest-path)
      (show-error! state "Target is unreachable")
      (animate!* state result))))

(defn board-table [state]
  (let [st @state
        {:board/keys [width height] :as board} (:db/board st)
        ;; For react performance, don't swap in every wall while
        ;; dragging, but rather natively animate them, store them in a
        ;; cache and flush them to the app db on mouse up.
        new-walls-cache (transient [])]
    [:table
     [:tbody
      (for [y (range height)]
        ^{:key y}
        [:tr
         (for [x (range width)
               :let [pos [x y]]]
           ^{:key x}
           [:td.cell
            {:id (cell-id pos)
             :class (for [[f v] {board/source? "cell--source"
                                 board/target? "cell--target"
                                 board/path? "cell--path"
                                 board/visited? "cell--visited"
                                 board/wall? "cell--wall"}
                          :when (f board [x y])]
                      v)
             :on-mouse-down #(let [type (cond (board/source? board pos) :drag/source
                                              (board/target? board pos) :drag/target
                                              :else :drag/wall)]
                               (swap! state assoc :db/dragging type)
                               (when (= :drag/wall type)
                                 (update-board! state (board/make-wall board pos))))
             :on-mouse-enter (fn []
                               (when-let [f (case (:db/dragging st)
                                              :drag/source board/set-source
                                              :drag/target board/set-target
                                              nil)]
                                 (update-board! state (f (:db/board st) pos)))
                               (when (= :drag/wall (:db/dragging st))
                                 (add-class! (cell-id pos) "cell--wall-animated")
                                 (conj! new-walls-cache pos)))
             :on-mouse-up (fn []
                            (swap! state dissoc :db/dragging)
                            (when-let [new-walls (seq (persistent! new-walls-cache))]
                              (update-board! state (reduce board/make-wall board
                                                           new-walls))))}])])]]))


(defn root [state]
  [:<>
   [:h1 "Pathfinder visualizer"]
   [:select {:on-change #(swap! state assoc :db/current-alg
                                (alg/from-name (.. % -target -value)))}
    (for [alg alg/ALL]
      ^{:key (::alg/key alg)} [:option (::alg/name alg)])]
   [:button {:on-click #(animate! state)} "Visualize!"]
   [:button {:on-click #(reset! state (new-db))} "Reset"]
   (when-let [e (:db/error @state)]
     [:p.u-error e])
   [board-table state]])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
