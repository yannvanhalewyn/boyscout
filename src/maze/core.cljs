(ns maze.core
  (:require [maze.board :as board]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn board-table [{:board/keys [width height] :as board}]
  (.log js/console board)
  [:table
   [:tbody
    (for [y (range height)]
      ^{:key y}
      [:tr
       (for [x (range width)
             :let [cell (get board [x y])]]
         (do
           (when (:cell/visited? cell)
             (.log js/console [x y]))
           ^{:key x}
           [:td.cell {:class (cond (:cell/start? cell) "cell--start"
                                   (:cell/end? cell) "cell--end"
                                   (:cell/visited? cell) "cell--visited")}]))])]])

(defn root [props]
  [:<>
   [:h1 "Pathfinder visualizer"]
   [board-table (-> (board/make 66 20)
                    (board/set-start [10 10])
                    (board/set-end [30 10])
                    (board/mark-visited [11 10])
                    (board/mark-visited [12 10])
                    (board/mark-visited [13 10])
                    (board/mark-visited [14 10])
                    (board/mark-visited [15 10])
                    (board/mark-visited [16 10])
                    (board/mark-visited [17 10])
                    (board/mark-visited [18 10])
                    (board/mark-visited [19 10]))]])

(defn ^:dev/after-load render! []
  (r/render [root] (.getElementById js/document "app")))

(defn main! []
  (render!))
