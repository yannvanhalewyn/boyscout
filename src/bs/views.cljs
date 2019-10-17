(ns bs.views
  (:require [bs.algorithm :as alg]))

(defn- checkmarks [{::alg/keys [weighted? shortest-path?]}]
  [(if weighted?
     {:check/checked? true
      :check/title "Weighted"
      :check/body (str "A weighted algorithm means that edges in the graph be "
                       "more costly to traverse than others.")}
     {:check/checked? false
      :check/title "Not weighted"
      :check/body "All edges have the same cost of traversal"})
   (if shortest-path?
     {:check/checked? true
      :check/title "Shortest path"
      :check/body "The result is guaranteed to be the shortest possible path."}
     {:check/checked? false
      :check/title "Any path"
      :check/body (str "The result is a correct path, but is not guaranteed to "
                       "be the shortest one.")})])

(defn algorithm-summary [{::alg/keys [name description] :as alg}]
  [:div
   [:div
    [:i.mdi.mdi-graph-outline.text-2xl.text-blue-800]
    [:h1.ml-1.inline-block.text-2xl.text-bold.text-gray-800 name]]
   [:p.mt-4.font-serif.text-justify.text-gray-700.leading-relaxed description]

   (for [check (checkmarks alg)]
     ^{:key (:check/title check)}
     [:div.mt-6.px-8.text-base
      [:i.mdi.text-lg.mr-3
       {:class (if (:check/checked? check)
                 "mdi-check text-green-500"
                 "mdi-close text-red-500")}]
      [:span.font-sans.text-gray-900.font-semibold.tracking-wider (:check/title check)]
      [:p.pl-8.mt-2.text-base.text-gray-700 (:check/body check)]])])
