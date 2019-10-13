(ns maze.maze
  (:require [maze.utils :as u]
            [clojure.set :as set]
            [clojure.core.async :as a
             :refer-macros [go go-loop]]))

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

(defn- fill-cell [ctx color [x y :as cell]]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx (* x CELL_SIZE) (* y CELL_SIZE) CELL_SIZE CELL_SIZE))

(defn- draw-dot [ctx color [x y :as cell]]
  (let [x (+ (* x CELL_SIZE) (/ CELL_SIZE 2))
        y (+ (* y CELL_SIZE) (/ CELL_SIZE 2))
        radius (int (/ CELL_SIZE 4))]
    (set! (.-fillStyle ctx) color)
    (.beginPath ctx)
    (.arc ctx x y radius 0 (* 2 Math/PI))
    (.closePath ctx)
    (.fill ctx)))

(declare neighbors)

(defn draw! [ctx maze]
  (set! (.-fillStyle ctx) "rgb(255,255,255)")
  (.fillRect ctx 0 0 SIZE SIZE)
  (fill-cell ctx "#5d5" [0 0])
  (fill-cell ctx "#d55" [(dec GRID) (dec GRID)])

  (set! (.-lineWidth ctx) WALL_SIZE)
  (set! (.-strokeStyle ctx) "#223")
  (.strokeRect ctx 0 0 SIZE SIZE)
  (doseq [[c1 edges] maze]
    (doseq [c2 (neighbors maze c1)]
      (when-not (contains? edges c2)
        (draw-wall ctx c1 c2)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maze

(defn- in-maze? [[x y]]
  (and (<= 0 x (dec GRID)) (<= 0 y (dec GRID))))

(defn- update-wall [f]
  (fn [maze c1 c2]
    (if (and c1 (in-maze? c2))
      (-> maze
          (update c1 f c2)
          (update c2 f c1))
      maze)))

(def add-path (update-wall conj))
(def remove-path (update-wall disj))

(defn- neighbors [maze [x y]]
  (filter in-maze? [[(inc x) y] [x (inc y)]
                    [(dec x) y] [x (dec y)]]))

(defn- empty-maze
  "A maze is a graph between cells, where the edges are a path. Edges
  are bidirectional, se they must appear twice.
  Example:
    {[0 0] #{[0 1] [1 0]}
     [0 1] #{[0 0]}
     [1 0] #{[0 0] [1 1]}
     [1 1] #{[1 0]}"
  []
  (zipmap (mapcat #(map vector (repeat %) (range GRID))
                  (range GRID))
          (repeat #{})))

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

;; Prim
(comment

  (def c (a/chan))

  (go-loop []
    (if-let [{:keys [maze connected path cur] :as v} (a/<! c)]
      (do (draw! ctx maze)
          (doseq [cell connected]
            (draw-dot ctx "green" cell))
          #_(doseq [cell path]
              (draw-dot ctx "orange" cell))
          (draw-dot ctx "blue" cur)
          (a/<! (a/timeout 5))
          (recur))
      (.log js/console "CLOSE")))

  (loop [maze (empty-maze)
         connected? #{[0 0]}
         path #{[0 0]}]
    (.clear js/console)
    (if (empty? path)
      maze
      (let [cur (last (vec path))
            [connected orphans] (u/split-by connected? (neighbors maze cur))
            connect-to (and (seq connected) (rand-nth connected))
            new-path (disj (apply conj path orphans) cur)
            new-maze (if connect-to (add-path maze cur connect-to) maze)
            new-connected? (conj connected? cur connect-to)]
        (a/put! c {:connected new-connected?
                   :path path
                   :maze new-maze
                   :cur cur
                   :connect-to connect-to
                   :conn-neighbors connected
                   :orphan-neighbors orphans})
        (recur new-maze
               (conj connected? cur connect-to)
               new-path))))

  (a/close! c)

  (.clear js/console)

  )

(def DRAW_TIMEOUT 10)

(defn main! []
  (let [c (a/chan)
        initial-maze (empty-maze)]
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
                    (let [new-maze (add-path maze from to)]
                      (a/put! c [new-maze from to])
                      new-maze))
                  :neighbors-fn (comp (partial sort-by #(rand-int 100)) neighbors)
                  :accumulator initial-maze})

    (a/close! c)))
