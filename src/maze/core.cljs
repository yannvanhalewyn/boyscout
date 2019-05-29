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
(set! (.-fillStyle ctx) "rgb(255,255,255)")
(set! (.-lineWidth ctx) 2)
(set! (.-strokeStyle ctx) "#223")

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

(defn draw! [ctx maze]
  (.fillRect ctx 0 0 SIZE SIZE)
  (.strokeRect ctx 0 0 SIZE SIZE)

  (doseq [[x row] (map-indexed vector maze)]
    (doseq [[y cell] ((map-indexed vector row))]
      (draw-cell ctx x y cell))))

(defn main! []
  (.log js/console "MAIN")
  (draw! ctx []))
