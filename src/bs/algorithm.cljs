(ns bs.algorithm
  (:require [bs.board :as board]
            [bs.algorithms.dijkstra :as dijkstra]
            [bs.algorithms.a-star :as a-star]
            [bs.algorithms.depth-first :as depth-first]
            [bs.utils :as u]))

(defn- source-url [symbol]
  (let [{:keys [file line]} (meta symbol)]
    (str "https://github.com/yannvanhalewyn/boyscout/blob/master/src/"
         file "#L" line)))

(def ALL
  [{::name "Dijkstra"
    ::key ::dijkstra
    ::weighted? true
    ::shortest-path? true
    ::wiki-url "https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm"
    ::img-url (u/resource-path "img/dijkstra.svg")
    ::source-url (source-url #'dijkstra/dijkstra)
    ::description
    (str "Dijkstra is the father of pathfinding algorithms. It was conceived by "
         "computer scientist Edsger W. Dijkstra published in 1956 and is one of the "
         "most famous pathfinding algorithms for weighted graphs, and is used in "
         "road networks, IP routing, geolocation, telephone networks and more.")}
   {::name "A*"
    ::key ::a*
    ::weighted? true
    ::shortest-path? true
    ::wiki-url "https://en.wikipedia.org/wiki/A*_search_algorithm"
    ::img-url (u/resource-path "img/a-star.svg")
    ::source-url (source-url #'a-star/a-star)
    ::description
    (str "A* (pronounced 'A Star') is considered the fastest pathfinding algorithm "
         "and is commonly used in video games. It is a variation on Dijkstra by "
         "using a heuristic to direct it's search towards the target. The heuristic "
         "should be an estimate of the distance from any node to the target. On "
         "this board we use the manhattan distance to direct the A* search.")}
   {::name "Depth First Search"
    ::short-name "DFS"
    ::key ::depth-first
    ::wiki-url "https://en.wikipedia.org/wiki/Depth-first_search"
    ::img-url (u/resource-path "img/depth-first.svg")
    ::source-url (source-url #'depth-first/depth-first)
    ::description
    (str "Depth First Search is one of the best known " "algorithms for searching "
         "tree or graph data " "structures. The algorithm will explore as far " "as "
         "possible along each branch before backtracking " "or until it reaches the "
         "target. Because of this, it " "is a bad choice for pathfinding.")}
   {::name "Breadth First Search"
    ::short-name "BFS"
    ::key ::breadth-first
    ::shortest-path? true
    ::wiki-url "https://en.wikipedia.org/wiki/Breadth-first_search"
    ::img-url (u/resource-path "img/breadth-first.svg")
    ::source-url (source-url #'depth-first/breadth-first)
    ::description
    (str "Breadth First Search is a very similar  algorithm " "to it's brother, "
         "Depth, differencing only in how " "the unvisited nodes are prioritised. "
         "Where in DFS " "a branch is fully explored until the end, Breadth " ""
         "First will explore every nearest neighbor before " "going deeper into any "
         "branch. This makes it more " "suitable for pathfinding.")}])

(defmulti process
  "Traverses the board graph using a specified algorithm. Will return
  a map with the order in which nodes were visited
  `:bs.algorithms/visitation-order`, and a list of coordinates forming
  the fastest path, effectively being the found solution
  `:bs.algorithms/path`"
  (fn [alg & _] alg))

(defn- process* [f {:board/keys [source target] :as board} & [visit-fn & args]]
  (let [visitation-order (transient [])
        visit-fn (fn [pos]
                   (conj! visitation-order pos)
                   ((or visit-fn board/neighbor-coords) board pos))
        result (apply f source target visit-fn args)]
    (when (and (= source (first result)) (= target (last result)))
      {::path result
       ::visitation-order (persistent! visitation-order)})))

(defmethod process ::dijkstra [_ board]
  (process*
   dijkstra/dijkstra board
   #(zipmap (board/neighbor-coords board %2) (repeat 1))))

(defmethod process ::a* [_ {:board/keys [target] :as board}]
  (process*
   a-star/a-star board
   #(zipmap (board/neighbor-coords board %2) (repeat 1))
   #(u/manhattan-distance % target)))

(defmethod process ::depth-first [_ board]
  (process* depth-first/depth-first board))

(defmethod process ::breadth-first [_ board]
  (process* depth-first/breadth-first board))
