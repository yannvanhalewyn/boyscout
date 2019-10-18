(ns public.img.generate
  (:require [dorothy.core :as dot]
            [dorothy.jvm :refer (save!)]))

(defn create-svg [filename data]
  (-> data
      (conj (dot/node-attrs {:fontname "sans-serif"}))
      (conj (dot/edge-attrs {:fontname "sans-serif"}))
      (conj (dot/graph-attrs {:overlap "false"
                              :splines "true"
                              :fontname "sans-serif"
                              :sep "+20"}))
      (dot/digraph)
      (dot/dot)
      (save! filename {:format :svg :layout :fdp})))

(defn- make-node [k & [type]]
  (let [[font text] (get {:target [2 3]
                          :path [2 3]
                          :visit [6 7]
                          nil ["black" "lightgray"]} type)]
    [k {:shape :Mcircle
        :style :filled
        :fontsize 10
        :colorscheme "paired12"
        :fontcolor (if type (mod font 12) font)
        :color (if type (mod text 12) text)
        :label (name k)}]))

(def DIJKSTRA
  (list (make-node "Start" :target)
        (make-node :A :visit)
        (make-node :B :path)
        (make-node :C :visit)
        (make-node :D :visit)
        (make-node :E)
        (make-node :F :visit)
        (make-node "End" :target)
        (make-node :H :path)
        (make-node :I)
        (make-node :J)
        (make-node :K)
        (make-node :L)
        ["Start" :A :D :F :H {:dir :none}]
        ["Start" :B :H "End" {}]
        [:A :B {:dir :none}]
        ["Start" :C :L :I :K :E "End" {:dir :none}]
        [:K :J :L {:dir :none}]))

(def A_STAR
  (list (make-node "Start" :target)
        (make-node :A)
        (make-node :B :path)
        (make-node :C)
        (make-node :D)
        (make-node :E)
        (make-node :F)
        (make-node "End" :target)
        (make-node :H :path)
        (make-node :I)
        (make-node :J)
        (make-node :K)
        (make-node :L)
        ["Start" :A :D :F :H {:dir :none}]
        ["Start" :B :H "End" {}]
        [:A :B {:dir :none}]
        ["Start" :C :L :I :K :E "End" {:dir :none}]
        [:K :J :L {:dir :none}]))

(def DFS
  (list (make-node "Start" :target)
        (make-node :A :visit)
        (make-node :D :visit)
        (make-node :F :visit)
        (make-node :B :path)
        (make-node :C)
        (make-node :E)
        (make-node "End" :target)
        ["Start" :A :D :F {:dir :none}]
        ["Start" :B "End" {}]
        [:A :B {:dir :none}]
        ["Start" :C :E "End" {:dir :none}]))

(def BFS
  (list (make-node "Start" :target)
        (make-node :A :visit)
        (make-node :D :visit)
        (make-node :F)
        (make-node :B :path)
        (make-node :C :visit)
        (make-node :E)
        (make-node "End" :target)
        ["Start" :A :D :F {:dir :none}]
        ["Start" :B "End" {}]
        [:A :B {:dir :none}]
        ["Start" :C :E "End" {:dir :none}]))

(doseq [[file data] [["resources/public/img/breadth-first.svg" BFS]
                     ["resources/public/img/depth-first.svg" DFS]
                     ["resources/public/img/dijkstra.svg" DIJKSTRA]
                     ["resources/public/img/a-star.svg" A_STAR]]]
  (create-svg file data)
  (clojure.java.shell/sh "open" "-a" "Google Chrome" file))
