(ns bs.core
  (:require [bs.board :as board]
            [bs.algorithms :as alg]
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
                   (board/set-target target))}))

(defn- show-error! [state err]
  (swap! state assoc :db/error err)
  (js/setTimeout #(swap! state dissoc :db/error) 2000))

(defn- update-board-from-algorithm [board result]
  (-> board
      (board/set-visited (::alg/visitation-order result))
      (board/set-path (::alg/shortest-path result))))

(defn- update-board!
  "Swaps the state with a new board. When it already has been
  animated, will recalculate the algorithm result."
  [state new-board]
  (if (seq (:board/path new-board))
    (let [result (alg/dijkstra new-board (:board/source new-board) (:board/target new-board))]
      (swap! state assoc :db/board (update-board-from-algorithm new-board result)))
    (swap! state assoc :db/board new-board)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(def SPEED 10)

(defn- animate!* [state {::alg/keys [shortest-path visitation-order] :as alg-result}]
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
  (let [{:board/keys [source target] :as board} (:db/board @state)
        {::alg/keys [shortest-path] :as result} (alg/dijkstra board source target)]
    (if (empty? shortest-path)
      (show-error! state "Target is unreachable")
      (animate!* state result))))

(defn board-table [state]
  (let [st @state
        {:board/keys [width height] :as board} (:db/board st)]
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
                                 (swap! state update :db/board board/make-wall pos)))
             :on-mouse-enter #(when-let [f (case (:db/dragging st)
                                             :drag/source board/set-source
                                             :drag/target board/set-target
                                             :drag/wall board/make-wall nil)]
                                (update-board! state (f (:db/board st) pos)))
             :on-mouse-up #(when (:db/dragging st)
                             (swap! state dissoc :db/dragging))}])])]]))


(defn root [state]
  [:<>
   [:h1 "Pathfinder visualizer"]
   [:button {:on-click #(animate! state)} "Visualize!"]
   [:button {:on-click #(reset! state (new-db))} "Reset"]
   (when-let [e (:db/error @state)]
     [:p.u-error e])
   [board-table state]])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
