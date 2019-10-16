(ns bs.algorithm
  (:require [bs.board :as board]
            [bs.algorithms.dijkstra :as dijkstra]
            [bs.algorithms.depth-first :as depth-first]))

(def ALL [{::key ::dijkstra      ::name "Dijkstra"}
          {::key ::depth-first   ::name "Depth First Search"}
          {::key ::breadth-first ::name "Breadth First Search"}])

(defmulti process
  "Traverses the board graph using a specified algorithm. Will return
  a map with the order in which nodes were visited
  `:bs.algorithms/visitation-order`, and a list of coordinates forming
  the fastest path, effectively being the found solution
  `:bs.algorithms/shortest-path`"
  (fn [alg & _] alg))

(defn- process* [f board source target & [neighbor-fn]]
  (let [visitation-order (transient [])
        result (f source target (fn [pos]
                                  (conj! visitation-order pos)
                                  ((or neighbor-fn board/neighbor-coords)
                                   board pos)))]
    (when (and (= source (first result)) (= target (last result)))
      {::shortest-path result
       ::visitation-order (persistent! visitation-order)})))

(defmethod process ::dijkstra [_ board source target]
  (process* dijkstra/dijkstra board source target
            #(zipmap (board/neighbor-coords board %2) (repeat 1))))

(defmethod process ::depth-first [_ board source target]
  (process* depth-first/depth-first board source target))

(defmethod process ::breadth-first [_ board source target]
  (process* depth-first/breadth-first board source target))
