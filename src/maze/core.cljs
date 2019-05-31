(ns maze.core
  (:require [clojure.set :as set]
            [clojure.core.async :as a
             :refer-macros [go go-loop]]))

(def SIZE 600)
(def GRID 30)
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

(defn- fill-cell [ctx color [x y :as cell]]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx (* x CELL_SIZE) (* y CELL_SIZE) CELL_SIZE CELL_SIZE))

(defn draw! [ctx maze]
  (set! (.-fillStyle ctx) "rgb(255,255,255)")
  (.fillRect ctx 0 0 SIZE SIZE)
  (fill-cell ctx "#5d5" [0 0])
  (fill-cell ctx "#d55" [(dec GRID) (dec GRID)])

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

(defn- add-edge [maze c1 c2 wall?]
  (if (and c1 (in-maze? c2))
    (-> (assoc-in maze [c1 c2] wall?)
        (assoc-in [c2 c1] wall?))
    maze))

(defn- neighbors [maze cell]
  (keys (get maze cell)))

(defn- empty-maze
  "A maze is a graph between cells, where the edges are a wall (boolean).
  Example:
    {[0 0] {[0 1] true}
     [0 1] {[1 1] true}
     [1 0] {[1 1] false}
     [1 1] {}"
  [& [all-walls?]]
  (let [all-cells (mapcat #(map vector (repeat %) (range GRID))
                          (range GRID))]
    (reduce
     (fn [maze [x y :as cell]]
       (-> maze
           (add-edge cell [(inc x) y] all-walls?)
           (add-edge cell [x (inc y)] all-walls?)))
     {} all-cells)))

(defn- random-maze []
  (let [maze (empty-maze)]
    (reduce-kv
     (fn [maze cell walls]
       (reduce-kv
        #(assoc-in %1 [cell %2] (rand-nth [true false]))
        maze walls))
     maze maze)))

(defn- depth-first
  "Traverses a graph via depth first. Accepts a visit-fn and a
  neighbors-fn to get the neighboring nodes given a certain node.

  For drawing paths in a maze this implementation keeps a stack of
  neighboring tuples `[from to]`, and calls the visit-fn with both,
  where `to` is the currently visited node, and `from` is the node
  which gave us that neighbor."
  [graph start {:keys [visit-fn neighbors-fn accumulator]}]
  (loop [stack (vector [nil start]) ;; [from to]
         visited? #{}
         acc accumulator]
    (if (empty? stack)
      (vec visited?)
      (let [[prev cur] (peek stack)
            visited? (conj visited? cur)
            neighbor-tuples (map #(vector cur %) (neighbors-fn graph cur))
            new-stack (into [] (remove (comp visited? second))
                            (into (pop stack) neighbor-tuples))]
        (recur new-stack visited? (visit-fn acc prev cur))))))

(def DRAW_TIMEOUT 5)

(defn main! []
  (let [c (a/chan)
        initial-maze (empty-maze true)]
    (js/console.clear)
    (draw! ctx initial-maze)

    (go-loop []
      (if-let [[maze from to] (a/<! c)]
        (do (draw! ctx maze)
            (fill-cell ctx "blue" to)
            (a/<! (a/timeout DRAW_TIMEOUT))
            (recur))
        (.log js/console "CLOSE")))

    (depth-first initial-maze [0 0]
                 {:visit-fn
                  (fn [maze from to]
                    (let [new-maze (add-edge maze from to false)]
                      (a/put! c [new-maze from to])
                      new-maze))
                  :neighbors-fn (comp (partial sort-by #(rand-int 100)) neighbors)
                  :accumulator initial-maze})

    (a/close! c)))
