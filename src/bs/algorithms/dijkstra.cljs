(ns bs.algorithms.dijkstra
  (:require [bs.utils :as u]
            [tailrecursion.priority-map :refer [priority-map-keyfn]]))

(defn- shortest-path
  "Walks back from the target node to the source node via the fastest
  parents in the costs map. To be used at the end of the dijkstra
  algorithm."
  [costs target]
  (loop [cur target
         result (list target)]
    (if-let [parent (get-in costs [cur :parent])]
      (recur parent (conj result parent))
      result)))

(defn dijkstra
  "Computes single-source shortest path distances in a directed graph.
  Given a node n, (neighbor-cost-fn n) should return a map with the
  connected nodes to n as keys and their (non-negative) distance from
  n as vals. (neighbor-cost-fn) can also return the keyword
  `:dijkstra/done`, in which case the algorithm will end early. This
  is useful when there is no need for the full sweep, but only to
  reach a certain node.
  Returns a map with all target nodes as keys, and a map
  with `:cost` and `:parent` as keys. The `:parent` can be used to
  walk back from any target node to the source node, as is necessary
  when using Dijkstra to compute the shortest path."
  [source target neighbor-costs-fn]
  (loop [distances (priority-map-keyfn :cost source {:cost 0})
         result {}]
    ;; Start on the nearest unvisited node (priority-map)
    (if-let [[cur current-distance] (peek distances)]
      (let [neighbor-costs (neighbor-costs-fn cur)
            ;; Add the current-node to the result set
            result (assoc result cur current-distance)]
        (if (= target cur)
          (shortest-path result target)
          ;; Recalculate cost of all unvisited (!) neighbors
          (let [neighbor-costs (-> neighbor-costs
                                   (u/remove-keys result)
                                   (u/map-vals (fn [cost]
                                                 {:cost (+ (:cost current-distance) cost)
                                                  :parent cur})))]
            ;; Merge in the new costs of those neighbors, but only if it's cheaper
            (recur (merge-with (partial min-key :cost) (pop distances) neighbor-costs)
                   result))))
      result)))
