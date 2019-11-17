(ns bs.board
  "This namespace provides a model of a board, which is represented as
  a bi-directional graph of cells. Walls are represented as cells
  having no connection to any neighbors.

  The data representation used for this board might look like this,
  given the following board:

  S = Source
  T = Target
  W = Wall   (no connections to neighbors)

       0   1   2
     +---+---+---+
   0 | S |   |   |
     +---+---+---+
   1 | W | W | T |
     +---+---+---+

  ``` clojure
  {:board/width 3
   :board/height 2
   :board/source [0 0]
   :board/target [2 1]
   :board/edges {[0 0] #{[0 1]}
                 [1 0] #{[0 0] [2 0]}
                 [2 0] #{[1 0] [2 1]}
                 [0 1] #{}
                 [1 1] #{}
                 [2 1] #{[2 0]}}}
  ```")

(defn- in-board?
  "Predicate for wether a position is on the board"
  [width height [x y]]
  (and (<= 0 x (dec width)) (<= 0 y (dec height))))

(defn- adjacent-coords
  "Returns all coords that are adjecent to pos and are inside the
  board."
  ([{:board/keys [width height]} pos]
   (adjacent-coords width height pos))
  ([width height [x y]]
   (filter (partial in-board? width height)
           [[x (dec y)]
            [(inc x) y]
            [x (inc y)]
            [(dec x) y]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph

(defn make
  "Makes a new board of given width and height, and where adjacent
  cells are connected."
  [width height]
  (let [coords (set (mapcat #(map vector (repeat %) (range height)) (range width)))]
    {:board/width width
     :board/height height
     :board/edges (reduce #(assoc %1 %2 (set (adjacent-coords width height %2)))
                          {} coords)}))

(defn all-coordinates [board]
  (keys (:board/edges board)))

(defn neighbor-coords [board pos]
  (get-in board [:board/edges pos]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Queries

(defn source? [board pos]
  (= pos (:board/source board)))

(defn target? [board pos]
  (= pos (:board/target board)))

(defn wall? [board pos]
  (empty? (get-in board [:board/edges pos])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mutations

(defn set-source [board pos]
  (assoc board :board/source pos))

(defn set-target [board pos]
  (assoc board :board/target pos))

(defn make-wall [board pos]
  (let [neighbors (neighbor-coords board pos)]
    (reduce
     #(update-in %1 [:board/edges %2] disj pos)
     (assoc-in board [:board/edges pos] #{}) neighbors)))

(defn destroy-wall [board pos]
  (let [neighbors (remove (partial wall? board) (adjacent-coords board pos))]
    (reduce
     #(update-in %1 [:board/edges %2] conj pos)
     (assoc-in board [:board/edges pos] (set neighbors)) neighbors)))
