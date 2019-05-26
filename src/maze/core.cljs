(ns maze.core)

(def SIZE 500)
(def GRID 20)
(def CELL_SIZE (/ SIZE GRID))

(defn init-canvas! []
  (.getContext (.getElementById js/document "canvas") "2d"))

(def ctx (init-canvas!))

(defn corners [x y]
  [[(* x CELL_SIZE) (* y CELL_SIZE)]
   [(* (inc x) CELL_SIZE) (* y CELL_SIZE)]
   [(* (inc x) CELL_SIZE) (* (inc y) CELL_SIZE)]
   [(* x CELL_SIZE) (* (inc y) CELL_SIZE)]])

(defn move-to [ctx [x y]] (.moveTo ctx x y))
(defn line-to [ctx [x y]] (.lineTo ctx x y))

(defn draw! [ctx cells]
  (set! (.-fillStyle ctx) "rgb(255,255,255)")
  (.fillRect ctx 0 0 SIZE SIZE)

  (set! (.-lineWidth ctx) 5)
  (set! (.-strokeStyle ctx) "#223")
  (.strokeRect ctx 0 0 SIZE SIZE)

  (doseq [x (range GRID)]
    (doseq [y (range GRID)]
      (let [[a b c d] (corners x y)]
        (.beginPath ctx)
        (move-to ctx d)
        (line-to ctx c)
        (line-to ctx b)
        (.stroke ctx)))))

(defn main! []
  (.log js/console "MAIN"))
