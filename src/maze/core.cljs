(ns maze.core
  (:require [clojure.set :as set]))

(defn- log [x]
  (.log js/console (clj->js x)))

(def SIZE 600)
(def GRID 20)
(def WALL_SIZE 2)
(def CELL_SIZE (/ SIZE GRID))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canvas / Drawing

(defn init-canvas! []
  (.getContext (.getElementById js/document "canvas") "2d"))

(def ctx (init-canvas!))

(defn move-to [ctx [x y]] (.moveTo ctx x y))
(defn line-to [ctx [x y]] (.lineTo ctx x y))

(defn draw-line [ctx line]
  (move-to ctx (first line))
  (line-to ctx (second line)))

(defn corners [[x y]]
  [[(* x       CELL_SIZE) (* y       CELL_SIZE)]
   [(* (inc x) CELL_SIZE) (* y       CELL_SIZE)]
   [(* (inc x) CELL_SIZE) (* (inc y) CELL_SIZE)]
   [(* x       CELL_SIZE) (* (inc y) CELL_SIZE)]])

(defn- draw-wall [ctx c1 c2]
  (.beginPath ctx)
  (let [line (set/intersection (set (corners c1)) (set (corners c2)))]
    (assert (= 2 (count line)) line)
    (draw-line ctx (vec line)))
  (.stroke ctx))

(defn draw! [ctx maze]
  (set! (.-fillStyle ctx) "rgb(255,255,255)")
  (.fillRect ctx 0 0 SIZE SIZE)
  (set! (.-fillStyle ctx) "#5d5")
  (.fillRect ctx 0 0 CELL_SIZE CELL_SIZE)
  (set! (.-fillStyle ctx) "#d55")
  (.fillRect ctx (- SIZE CELL_SIZE) (- SIZE CELL_SIZE) CELL_SIZE CELL_SIZE)

  (set! (.-lineWidth ctx) WALL_SIZE)
  (set! (.-strokeStyle ctx) "#223")
  (.strokeRect ctx 0 0 SIZE SIZE)
  (doseq [[c1 edges] maze]
    (doseq [[c2 wall?] edges]
      (when wall? (draw-wall ctx c1 c2)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maze

(defn- in-maze? [[x y]]
  (and (< x GRID) (< y GRID)))

(defn- add-edge [maze c1 c2]
  (if (in-maze? c2)
    (assoc-in maze [c1 c2] false)
    maze))

(defn- empty-maze
  "A maze is a graph between cells, where the edges are a wall (boolean).
  Example:
    {[0 0] {[0 1] true}
     [0 1] {[1 1] true}
     [1 0] {[1 1] false}
     [1 1] {}"
  []
  (let [all-cells (mapcat #(map vector (repeat %) (range GRID))
                          (range GRID))]
    (reduce
     (fn [maze [x y :as cell]]
       (-> maze
           (add-edge cell [(inc x) y])
           (add-edge cell [x (inc y)])))
     {} all-cells)))

(defn- random-maze []
  (let [maze (empty-maze)]
    (reduce-kv
     (fn [maze cell walls]
       (reduce-kv
        #(assoc-in %1 [cell %2] (rand-nth [true false]))
        maze walls))
     maze maze)))

(defn main! []
  (draw! ctx (random-maze)))
