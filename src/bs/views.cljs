(ns bs.views
  (:require [bs.algorithm :as alg]
            [bs.db :as db]
            [reagent.core :as r]))

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

(defn algorithm-summary []
  (let [modal? (r/atom true)]
    (fn [state]
      (let [{:db/keys [current-alg]} @state]
        [:div
         [:div.w-full.flex.justify-between.items-center
          [:div
           [:i.mdi.mdi-graph-outline.text-2xl.text-blue-800]
           [:h1.ml-1.inline-block.text-2xl.text-bold.text-gray-800
            (::alg/name current-alg)]]
          [:a.u-link {:on-click #(reset! modal? true)} "Change"]]
         [:img.float-left.p-3.mt-3.w-32.h-32 {:src (::alg/img-url current-alg)}]
         [:p.mt-4.font-serif.text-justify.text-gray-700.leading-relaxed
          (::alg/description current-alg)]
         (for [check (checkmarks current-alg)]
           ^{:key (:check/title check)}
           [:div.mt-6.px-8.text-base
            [:i.mdi.text-lg.mr-3
             {:class (if (:check/checked? check)
                       "mdi-check text-green-500"
                       "mdi-close text-red-500")}]
            [:span.font-sans.text-gray-900.font-semibold.tracking-wider (:check/title check)]
            [:p.pl-8.mt-2.text-base.text-gray-700 (:check/body check)]])
         (when @modal?
           [:div.modal.fixed.w-full.h-full.top-0.left-0.flex.items-center.justify-center
            {:on-click #(reset! modal? false)}
            [:div.absolute.w-full.h-full.bg-gray-900.opacity-50]
            [:div.bg-white.md:max-w-6xl.mx-auto.rounded.shadow-lg.z-50.overflow-y-auto
             [:h1.text-3xl.py-4.px-10.text-blue-800.shadow-inner.shadow-2xl.bg-gray-200 "Pick algorithm"]
             [:div.py-4.text-left.px-6
              [:div.flex
               (for [{::alg/keys [key name description img-url] :as alg} alg/ALL]
                 ^{:key key}
                 [:a.p-4.relative.cursor-default
                  {:on-click #(db/update! state assoc :db/current-alg alg)}
                  [:img.float-right.pl-4.w-32.h-32 {:src img-url}]
                  [:h1.text-xl.text-bold.text-gray-800 name]
                  [:p.font-serif.text-gray-700.mb-12.mt-2.leading-relaxed.tracking-wide description]
                  [:div.absolute.w-full.bottom-0.text-center
                   [:button.px-4.bg-transparent.py-3.rounded-lg
                    {:class (if (= key (::alg/key current-alg))
                              "text-gray-400 cursor-default"
                              "text-indigo-500 hover:text-indigo-400 hover:bg-gray-100")}
                    "Select"]]])]
              [:button.float-right.px-4.bg-indigo-500.p-3.rounded-lg.text-white.hover:bg-indigo-400
               {:on-click #(reset! modal? false)}
               "Close"]
              [:div.clearfix]]]])]))))
