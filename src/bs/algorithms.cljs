(ns bs.algorithms
  (:require [clojure.set :as set]
            [bs.board :as board]))

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

(defn- recalculate-costs [costs cur nodes]
  (reduce
   (fn [costs node]
     (let [new-cost (min-key :cost (get costs node)
                             {:cost (inc (get-in costs [cur :cost]))
                              :parent cur})]
       (assoc costs node new-cost)))
   costs nodes))

(defn dijkstra
  "Traverses the board graph using the dijkstra algorithm. Will return
  a map with the order in which nodes were visited
  `:bs.algorithms/visitation-order`, and a list of coordinates forming
  the fastest path, effectively being the found solution
  `:bs.algorithms/shortest-path`"
  [board source target]
  (loop [unvisited (set (board/all-coordinates board))
         costs (assoc (zipmap unvisited (repeat {:cost js/Infinity}))
                 source {:cost 0})
         cur source
         result {::visitation-order []}]
    (if-not (contains? unvisited target)
      (when (and (seq (::visitation-order result))
                 (not (= js/Infinity (get-in costs [target :cost]))))
        (assoc result ::shortest-path (shortest-path costs target)))
      (let [unvisited-neighbors (set/intersection (board/neighbor-coords board cur)
                                                  unvisited)
            costs (recalculate-costs costs cur unvisited-neighbors)
            unvisited (disj unvisited cur)
            next (apply min-key (comp :cost costs) unvisited)]
        (recur unvisited
               costs
               next
               (update result ::visitation-order conj cur))))))
