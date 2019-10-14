(ns maze.core
  (:require [maze.board :as board]
            [maze.algorithms :as alg]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(defn- new-db []
  (let [[src target] [[10 10] [30 15]]]
    {:db/board (-> (board/make 66 20)
                   (board/set-source src)
                   (board/set-target target))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(def SPEED 10)

(defn- animate! [state]
  (let [{:board/keys [source target] :as board} (:db/board @state)
        {::alg/keys [visitation-order fastest-path]} (alg/dijkstra board source target)]
    (doseq [[i node] (map-indexed vector visitation-order)]
      (js/setTimeout
       #(swap! state update :db/board board/mark-visited node)
       (* i SPEED)))
    (doseq [[i node] (map-indexed vector fastest-path)]
      (js/setTimeout
       #(swap! state update :db/board board/mark-path node)
       (+ (* i SPEED 4) (* (count visitation-order) SPEED))))))

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
            {:class (for [[f v] {board/source? "cell--source"
                                 board/target? "cell--target"
                                 board/path? "cell--path"
                                 board/visited? "cell--visited"
                                 board/wall? "cell--wall"}
                          :when (f board [x y])]
                      v)
             :on-mouse-down #(cond
                               (board/source? board pos)
                               (swap! state assoc :db/dragging :drag/source)
                               (board/target? board pos)
                               (swap! state assoc :db/dragging :drag/target)
                               :else
                               (swap! state update :db/board board/make-wall pos))
             :on-mouse-enter (when-let [f (case (:db/dragging st)
                                            :drag/source board/set-source
                                            :drag/target board/set-target nil)]
                               #(swap! state update :db/board f pos))
             :on-mouse-up (when (:db/dragging st)
                            #(swap! state dissoc :db/dragging))}])])]]))

(defn root [state]
  [:<>
   [:h1 "Pathfinder visualizer"]
   [:button {:on-click #(animate! state)} "Visualize!"]
   [:button {:on-click #(reset! state (new-db))} "Reset"]
   [board-table state]])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
