(ns bs.board
  "This namespace provides a model of a board, which is represented as a
  bi-directional weighted graph of cells. Walls are represented as
  cells having no connection to any neighbors.

  The data representation used for this board might look like this,
  given the following board:

  S = Source
  T = Target
  F = Forest (heavier weight)
  W = Wall   (no connections to neighbors)

       0   1   2
     +---+---+---+
   0 | S | F |   |
     +---+---+---+
   1 |   | W | T |
     +---+---+---+

  ``` clojure
  {:board/width 3
   :board/height 2
   :board/source [0 0]
   :board/target [2 1]
   :board/edges {[0 0] {[0 1] 1
                        [1 0] 2}
                 [0 1] {[0 0] 2
                        [1 2] 2
                        [1 1] 2}
                 [0 2] {[1 0] 2
                        [2 1] 1}
                 [1 0] {[0 0] 1}
                 [1 1] {}
                 [1 2] {[2 0] 1}}}
  ```"
  (:require [bs.utils :as u]))


(def DEFAULT_WEIGHT 1)
(def FOREST_WEIGHT 2)

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
      :board/edges (reduce #(assoc %1 %2 (zipmap (adjacent-coords board %2)
                                                 (repeat DEFAULT_WEIGHT)))
                           {} coords))))

(defn all-coordinates [board]
  (keys (:board/edges board)))

(defn neighbors [board pos]
  (get-in board [:board/edges pos]))

(def neighbor-coords (comp keys neighbors))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Queries

(defn source? [board pos]
  (= pos (:board/source board)))

(defn target? [board pos]
  (= pos (:board/target board)))

(defn wall? [board pos]
  (empty? (get-in board [:board/edges pos])))

(defn forest? [{:board/keys [edges] :as board} pos]
  (when-let [weights (seq (map #(get-in edges [% pos])
                               (neighbor-coords board pos)))]
    (every? #(not= DEFAULT_WEIGHT %) weights)))

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
     #(update-in %1 [:board/edges %2] dissoc pos)
     (assoc-in board [:board/edges pos] {}) neighbors)))

(defn set-weight
  "Sets the weight between from and to"
  [board from to weight]
  (assoc-in board [:board/edges from to] weight))

(defn make-forest [board pos]
  (->> (neighbor-coords board pos)
       (remove (partial wall? board))
       (reduce #(set-weight %1 %2 pos FOREST_WEIGHT) board)))

(defn destroy-wall [board pos]
  (let [weight #(if (forest? board %) FOREST_WEIGHT DEFAULT_WEIGHT)]
    (->> (adjacent-coords board pos)
         (remove (partial wall? board))
         (reduce #(-> %1
                      (set-weight pos %2 (weight %2))
                      (set-weight %2 pos DEFAULT_WEIGHT))
                 board))))

(defn unilever "Destroys forests" [board pos]
  (reduce #(set-weight %1 %2 pos DEFAULT_WEIGHT)
          board (neighbor-coords board pos)))

(defn reset-edges [{:board/keys [width height] :as board}]
  (assoc board :board/edges (:board/edges (make width height))))
