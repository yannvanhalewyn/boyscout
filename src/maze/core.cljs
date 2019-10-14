(ns maze.core
  (:require [maze.board :as board]
            [maze.algorithms :as alg]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn board-table [{:board/keys [width height] :as board}]
  [:table
   [:tbody
    (for [y (range height)]
      ^{:key y}
      [:tr
       (for [x (range width)
             :let [cell (get board [x y])]]
         ^{:key x}
         [:td.cell {:class (cond (:cell/start? cell) "cell--start"
                                 (:cell/end? cell) "cell--end"
                                 (:cell/path? cell) "cell--path"
                                 (:cell/visited? cell) "cell--visited")}])])]])

(defn root [{:keys [width height source target]}]
  (let [board (-> (board/make width height)
                  (board/set-start source)
                  (board/set-end target))
        result (alg/dijkstra board source target)]
    [:<>
     [:h1 "Pathfinder visualizer"]
     [board-table (reduce
                   board/mark-path
                   (reduce board/mark-visited board (::alg/visitation-order result))
                   (::alg/fastest-path result))]]))

(defn ^:dev/after-load render! []
  (r/render [root {:width 66
                   :height 20
                   :source [10 10]
                   :target [30 10]}]
    (.getElementById js/document "app")))

(defn main! []
  (render!))
