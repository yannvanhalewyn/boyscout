(ns maze.algorithms
  (:require [clojure.set :as set]
            [maze.board :as board]))

(defn- fastest-paths
  "Walks back from the end node to the start node via the fastest
  parents in the costs map. To be used at the end of the dijkstra
  algorithm."
  [costs end]
  (loop [cur end
         result (list end)]
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
  `:maze.algorithms/visitation-order`, and a list of coordinates forming
  the fastest path, effectively being the found solution
  `:maze.algorithms/fastest-path`"
  [board start end]
  (loop [unvisited (set (board/all-coordinates board))
         costs (assoc (zipmap unvisited (repeat {:cost js/Infinity}))
                 start {:cost 0})
         cur start
         result {::visitation-order []}]
    (if-not (contains? unvisited end)
      (assoc result ::fastest-path (fastest-paths costs end))
      (let [unvisited-neighbors (set/intersection (board/neighbor-coords board cur)
                                                  unvisited)
            costs (recalculate-costs costs cur unvisited-neighbors)
            unvisited (disj unvisited cur)
            next (first (sort-by (comp :cost costs) unvisited))]
        (recur unvisited
               costs
               next
               (update result ::visitation-order conj cur))))))
