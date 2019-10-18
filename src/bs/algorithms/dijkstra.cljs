(ns bs.algorithms.dijkstra
  (:require [bs.algorithms.a-star :as a-star]))

(defn dijkstra
  "Computes single-source shortest path distances in a directed graph.
  Given a node n, (visit-fn n) should return a map with the
  connected nodes to n as keys and their (non-negative) distance from
  n as vals."
  [source target visit-fn]
  (a-star/a-star source target visit-fn (constantly 0)))
