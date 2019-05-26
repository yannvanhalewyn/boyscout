(ns maze.core)

(def SIZE 500)
(def GRID 20)
(def CELL_SIZE (/ SIZE GRID))

(defn init-canvas! []
  (.getContext (.getElementById js/document "canvas") "2d"))

(defn move-to [ctx [x y]] (.moveTo ctx x y))
(defn line-to [ctx [x y]] (.lineTo ctx x y))

(def ctx (init-canvas!))

(def CELLS [#{:east :south} #{:east} #{:south} #{}])

(defn corners [x y]
  [[(* x CELL_SIZE) (* y CELL_SIZE)]
   [(* (inc x) CELL_SIZE) (* y CELL_SIZE)]
   [(* (inc x) CELL_SIZE) (* (inc y) CELL_SIZE)]
   [(* x CELL_SIZE) (* (inc y) CELL_SIZE)]])

(defn draw-cell [ctx x y walls]
  (.beginPath ctx)
  (doseq [wall walls]
    (case wall
      :east (do (move-to ctx [(* (inc x) CELL_SIZE) (* y CELL_SIZE)])
                (line-to ctx [(* (inc x) CELL_SIZE) (* (inc y) CELL_SIZE)]))
      :south (do (move-to ctx [(* x CELL_SIZE) (* (inc y) CELL_SIZE)])
                 (line-to ctx [(* (inc x) CELL_SIZE) (* (inc y) CELL_SIZE)]))))
  (.stroke ctx))

(defn draw! [ctx cells]
  (set! (.-fillStyle ctx) "rgb(255,255,255)")
  (.fillRect ctx 0 0 SIZE SIZE)

  (set! (.-lineWidth ctx) 3)
  (set! (.-strokeStyle ctx) "#223")
  (.strokeRect ctx 0 0 SIZE SIZE)

  (doseq [x (range GRID)]
    (doseq [y (range GRID)]
      (draw-cell ctx x y (rand-nth CELLS)))))

(defn main! []
  (.log js/console "MAIN")
  (draw! ctx []))
