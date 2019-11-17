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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Board graph

(defn make [width height]
  ;; Steps needed and pseudo code:
  ;; 1. Generate all coordinates in a board of width and height
  ;;    tips:
  ;;      - (range width) will give you a range between 0 and width-1.
  ;;      - For (range width), generate a tuple of [width h] for every
  ;;        height in (range height)
  ;; 2. Given a point, what are it's neighbors?
  ;;    (neighboring-coords [1 1]) => [[1 0] [2 1] [1 3] [0 1]]
  ;; 3. Filter the neighbors that are outside of the board
  ;;    (in-board? width height [-1 0]) => false
  ;;    (in-board? 10 10 [11 11])       => false
  ;;    (in-board? 10 10 [4 3])         => true
  ;; 4. Build the edges as a map from every coordinate to it's neighbors.
  ;;    (reduce
  ;;      (fn [edges pos]
  ;;        (assoc edges pos (adjacent-coords pos))
  ;;      {} all-coords)
  )

(defn all-coordinates [board])
(defn neighbor-coords [board pos])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Queries

(defn source? [board pos])
(defn target? [board pos])
(defn wall? [board pos])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mutations

(defn set-source [board pos])
(defn set-target [board pos])
(defn make-wall [board pos])
(defn destroy-wall [board pos])
