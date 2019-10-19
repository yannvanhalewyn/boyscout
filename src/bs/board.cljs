(ns bs.board
  "This namespace provides a model of a board, which is represented as a
  bi-directional graph of cells. Walls can be added to the edges, and
  properties of the cell to the nodes. The data representation used
  for this board might look like this:

  ``` clojure
  {:board/width 10
   :board/height 10
   :board/source [1 1]
   :board/target [1 0]
   :board/edges {[0 0] #{[0 1] [1 0]}
                 [0 1] #{[1 1] [0 0]}
                 [1 0] #{[0 0] [1 1]}
                 [1 1] #{[0 1] [1 0]}}}
  ```")

(defn- in-board?
  "Predicate for wether a position is on the board"
  [{:board/keys [width height]} [x y]]
  (and (<= 0 x (dec width)) (<= 0 y (dec height))))

(defn- adjacent-coords
  "Returns all coords that are adjecent to pos and are inside the
  board."
  [board [x y]]
  (filter (partial in-board? board) [[x (dec y)]
                                     [(inc x) y]
                                     [x (inc y)]
                                     [(dec x) y]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph

(defn make
  "Makes a new board of given width and height, and where adjacent
  cells are connected."
  [width height]
  (let [coords (set (mapcat #(map vector (repeat %) (range height)) (range width)))
        board {:board/width width
               :board/height height}]
    (assoc board
      :board/edges (reduce #(assoc %1 %2 (set (adjacent-coords board %2)))
                           {} coords))))

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

(defn cell-id
  "A unique string id for that cell, useful for say an html-id"
  [[x y]]
  (str "cell-" x "-" y))

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

(defn clear-walls [{:board/keys [width height] :as board}]
  (assoc board :board/edges (:board/edges (make width height))))
