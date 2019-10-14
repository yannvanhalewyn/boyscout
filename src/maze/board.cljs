(ns maze.board
  "This namespace provides a model of a board, which is a
  bi-directional graph of cells. Walls can be added to the edges, and
  properties of the cell to the nodes. The data representation used
  for this board might look like this:

  ``` clojure
  {[0 0] {:cell/start true
          :cell/neighbors #{[0 1] [1 0]}}
   [0 1] {:cell/visited? true
          :cell/neighbors #{[1 1] [0 0]}}
   [1 0] {:cell/neighbors #{[0 0] [1 1]}}
   [1 1] {:cell/end true
          :cell/neighbors #{[0 1] [1 0]}}}
  ```"
  (:require [clojure.set :as set]))

(defn make [width height]
  (let [coords (set (mapcat #(map vector (repeat %) (range height)) (range width)))]
    (reduce
     (fn [out [x y :as pos]]
       (let [neighbors (set/intersection coords #{[x (dec y)]
                                                  [(inc x) y]
                                                  [x (inc y)]
                                                  [(dec x) y]})]
         (assoc out pos {:cell/neighbors neighbors})))
     {:board/width width :board/height height}
     coords)))

(defn all-coordinates [board]
  (keys (dissoc board :board/width :board/height)))

(defn neighbor-coords [board pos]
  (get-in board [pos :cell/neighbors]))

(defn- update-cell [board pos f & args]
  (apply update board pos f args))

(defn- mark-cell [k board pos]
  (update-cell board pos assoc k true))

(def set-start    (partial mark-cell :cell/start?))
(def set-end      (partial mark-cell :cell/end?))
(def mark-visited (partial mark-cell :cell/visited?))
(def mark-path    (partial mark-cell :cell/path?))
