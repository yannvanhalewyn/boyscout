(ns maze.core
  (:require [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Board

(defn make-board [x y]
  (vec
   (for [i (range y)]
     (vec
      (for [j (range x)]
        (if (= [i j] [10 10])
          {:cell/start true}
          {}))))))

(defn- update-cell [board [x y] f & args]
  (apply update-in board [y x] f args))

(defn- mark-cell [k board coord]
  (update-cell board coord assoc k true))

(def set-start    (partial mark-cell :cell/start))
(def set-end      (partial mark-cell :cell/end))
(def mark-visited (partial mark-cell :cell/visited?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn grid [{:keys [board]}]
  [:table
   [:tbody
    (for [[i row] (map-indexed vector board)]
      ^{:key i}
      [:tr
       (for [[i cell] (map-indexed vector row)]
         ^{:key i}
         [:td.cell {:class (cond (:cell/start cell) "cell--start"
                                 (:cell/end cell) "cell--end"
                                 (:cell/visited? cell) "cell--visited")}])])]])

(defn root [props]
  [:<>
   [:h1 "Pathfinder visualizer"]
   [grid {:board (-> (make-board 66 20)
                     (set-start [10 10])
                     (set-end [30 10])
                     (mark-visited [11 10])
                     (mark-visited [12 10])
                     (mark-visited [13 10])
                     (mark-visited [14 10])
                     (mark-visited [15 10])
                     (mark-visited [16 10])
                     (mark-visited [17 10])
                     (mark-visited [18 10])
                     (mark-visited [19 10]))}]])

(defn ^:dev/after-load render! []
  (r/render [root] (.getElementById js/document "app")))

(defn main! []
  (render!))
