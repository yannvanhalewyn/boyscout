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

(defn- process* [f board source target]
  (let [visitation-order (transient [])
        result (f source target (fn [pos]
                                  (conj! visitation-order pos)
                                  (board/neighbor-coords board pos)))]
    (when (and (= source (first result)) (= target (last result)))
      {::shortest-path result
       ::visitation-order (persistent! visitation-order)})))

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
  (process* depth-first/depth-first board source target))

(defmethod process ::breadth-first [_ board source target]
  (process* depth-first/breadth-first board source target))
