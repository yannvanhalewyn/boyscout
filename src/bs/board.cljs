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
  ```"
  (:require [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph

(defn make [width height]
  (let [coords (set (mapcat #(map vector (repeat %) (range height)) (range width)))]
    {:board/width width
     :board/height height
     :board/edges
     (reduce
      (fn [out [x y :as pos]]
        (let [neighbors (set/intersection coords #{[x (dec y)]
                                                   [(inc x) y]
                                                   [x (inc y)]
                                                   [(dec x) y]})]
          (assoc out pos neighbors)))
      {} coords)}))

(defn all-coordinates [board]
  (keys (:board/edges board)))

(defn neighbor-coords [board pos]
  (get-in board [:board/edges pos]))

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

(defn make-walls [board positions]
  (reduce make-wall board positions))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Queries

(defn source? [board pos]
  (= pos (:board/source board)))

(defn target? [board pos]
  (= pos (:board/target board)))

(defn wall? [board pos]
  (empty? (get-in board [:board/edges pos])))

(defn cell-id
  "The id used to refer to this cell's html node"
  [[x y]]
  (str "cell-" x "-" y))
