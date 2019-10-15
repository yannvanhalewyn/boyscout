(ns bs.algorithm
  (:require [bs.board :as board]
            [bs.algorithms.dijkstra :as dijkstra]
            [bs.algorithms.depth-first :as depth-first]))

(def ALL [::dijkstra ::depth-first])

(defmulti process
  "Traverses the board graph using a specified algorithm. Will return
  a map with the order in which nodes were visited
  `:bs.algorithms/visitation-order`, and a list of coordinates forming
  the fastest path, effectively being the found solution
  `:bs.algorithms/shortest-path`"
  (fn [alg & _] alg))

(defmethod process ::dijkstra [_ board source target]
  (let [visitation-order (transient [])
        neighbor-cost-fn (fn [pos]
                           (conj! visitation-order pos)
                           (if (= pos target)
                             :dijkstra/done
                             (zipmap (board/neighbor-coords board pos) (repeat 1))))
        costs (dijkstra/dijkstra source neighbor-cost-fn)
        path (dijkstra/shortest-path costs target)]
    (when (and (= source (first path)) (= target (last path)))
      {::shortest-path path
       ::visitation-order (persistent! visitation-order)})))

(defmethod process ::depth-first [_ board source target]
  (let [result (depth-first/depth-first source #(if (= % target)
                                                  :depth-first/done
                                                  (board/neighbor-coords board %)))]
    (when (= target (last result))
      {::shortest-path result
       ::visitation-order result})))
