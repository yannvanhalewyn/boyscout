(ns maze.core
  (:require [maze.board :as board]
            [maze.algorithms :as alg]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(defn- new-db []
  (let [[src target] [[10 10] [30 15]]]
    {:db/board (-> (board/make 66 20)
                   (board/set-start src)
                   (board/set-end target))
     :db/source src
     :db/target target}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(def SPEED 10)

(defn- animate! [state]
  (let [{:db/keys [board source target]} @state
        {::alg/keys [visitation-order fastest-path]} (alg/dijkstra board source target)]
    (doseq [[i node] (map-indexed vector visitation-order)]
      (js/setTimeout
       #(swap! state update :db/board board/mark-visited node)
       (* i SPEED)))
    (doseq [[i node] (map-indexed vector fastest-path)]
      (js/setTimeout
       #(swap! state update :db/board board/mark-path node)
       (+ (* i SPEED 4) (* (count visitation-order) SPEED))))))

(defn board-table [{:board/keys [width height] :as board}]
  [:table
   [:tbody
    (for [y (range height)]
      ^{:key y}
      [:tr
       (for [x (range width)
             :let [cell (get board [x y])]]
         ^{:key x}
         [:td.cell {:class (for [[k v] {:cell/start? "cell--start"
                                        :cell/end? "cell--end"
                                        :cell/path? "cell--path"
                                        :cell/visited? "cell--visited"}
                                 :when (get cell k)]
                             v)}])])]])

(defn root [state]
  [:<>
   [:h1 "Pathfinder visualizer"]
   [:button {:on-click #(animate! state)} "Visualize!"]
   [:button {:on-click #(reset! state (new-db))} "Reset"]
   [board-table (:db/board @state)]])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
