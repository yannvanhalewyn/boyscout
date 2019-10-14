(ns maze.core
  (:require [maze.board :as board]
            [maze.algorithms :as alg]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(def SPEED 10)

(defn- animate! [state]
  (let [{:db/keys [board source target]} @state
        result (alg/dijkstra board source target)]
    (doseq [[i node] (map-indexed vector (::alg/visitation-order result))]
      (js/setTimeout
       #(swap! state update :db/board board/mark-visited node)
       (* i SPEED)))
    (doseq [[i node] (map-indexed vector (::alg/fastest-path result))]
      (js/setTimeout
       #(swap! state update :db/board board/mark-path node)
       (+ (* i SPEED 4) (* (count (::alg/visitation-order result)) SPEED))))))

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
   [board-table (:db/board @state)]])

(defn ^:dev/after-load render! []
  (let [[src target] [[10 10] [30 3]]
        state {:db/board (-> (board/make 66 20)
                             (board/set-start src)
                             (board/set-end target))
               :db/source src
               :db/target target}]
    (r/render [root (r/atom state)] (.getElementById js/document "app"))))

(defn main! []
  (render!))
