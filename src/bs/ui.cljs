(ns bs.ui
  (:require [bs.algorithm :as alg]
            [bs.board :as board]
            [bs.db :as db]
            [bs.utils :as u]
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

(defn- select-algorithm-modal [{:keys [on-close on-change current-alg]}]
  [:div.modal.fixed.w-full.h-full.top-0.left-0.flex.items-center.justify-center
   {:on-click on-close}
   [:div.absolute.w-full.h-full.bg-gray-900.opacity-50]
   [:div.bg-white.md:max-w-6xl.mx-auto.rounded.shadow-lg.z-50.overflow-y-auto
    [:h1.text-3xl.py-4.px-10.text-blue-800.shadow-inner.shadow-2xl.bg-gray-200 "Pick algorithm"]
    [:div.py-4.text-left.px-6
     [:div.flex
      (for [{::alg/keys [key name description img-url] :as alg} alg/ALL]
        ^{:key key}
        [:a.p-4.relative.cursor-default
         {:on-click #(on-change alg alg)}
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
      {:on-click on-close}
      "Close"]
     [:div.clearfix]]]])

(defn algorithm-summary
  "A little side panel on the left with the algorithm name description
  and some facts. Can also open a modal and change the selected algorithm"
  []
  (let [modal? (r/atom false)]
    (fn [state]
      (let [{:db/keys [current-alg]} @state]
        [:div
         [:div.w-full.flex.justify-between.items-center
          [:div
           [:i.mdi.mdi-graph-outline.text-2xl.text-blue-800]
           [:h1.ml-1.inline-block.text-2xl.text-bold.text-gray-800
            (::alg/name current-alg)]]
          (when-not (db/animating? @state)
            [:a.u-link {:on-click #(reset! modal? true)} "Change"])]
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
           [select-algorithm-modal {:on-close #(reset! modal? false)
                                    :on-change #(db/update! state assoc :db/current-alg %)
                                    :current-alg current-alg}])]))))

(defn board-table
  "The main animating attraction"
  [state]
  (let [{:db/keys [board alg-result] :as st} @state
        {:board/keys [width height] :as board} board
        animating? (db/animating? st)
        {::alg/keys [path visitation-order]} (when-not animating? alg-result)
        path? (let [s (set path)] #(contains? s %2))
        visited? (let [s (set visitation-order)] #(contains? s %2))
        ;; For react performance, don't swap in every wall while
        ;; dragging, but rather natively animate them, store them in a
        ;; cache and flush them to the app db on mouse up.
        new-walls-cache (transient [])

        start-drag! (fn [pos]
                      (let [type (cond (board/source? board pos) :drag/source
                                       (board/target? board pos) :drag/target
                                       :else :drag/wall)]
                        (db/update! state assoc :db/dragging type)
                        (when (= :drag/wall type)
                          (db/update! state update :db/board board/make-wall pos))))
        drag-to!     (fn [pos]
                       (when-let [f (case (:db/dragging st)
                                      :drag/source board/set-source
                                      :drag/target board/set-target
                                      nil)]
                         (db/update! state update :db/board f pos))
                       (when (= :drag/wall (:db/dragging st))
                         (u/add-class! (board/cell-id pos) "cell--wall-animated")
                         (conj! new-walls-cache pos)))
        end-drag!     (fn []
                        (when (contains? st :db/dragging)
                          (db/update! state dissoc :db/dragging)
                          (when-let [new-walls (seq (persistent! new-walls-cache))]
                            (db/update! state update :db/board board/make-walls new-walls))))]
    [:table {:on-mouse-leave end-drag!}
     [:tbody {:class (when animating? "cursor-not-allowed")}
      (for [y (range height)]
        ^{:key y}
        [:tr
         (for [x (range width)
               :let [pos [x y]]]
           ^{:key x}
           [:td.cell
            {:id (board/cell-id pos)
             :class (for [[f v] {board/source? "cell--source"
                                 board/target? "cell--target"
                                 board/wall? "cell--wall"
                                 path? "cell--path"
                                 visited? "cell--visited"}
                          :when (f board [x y])] v)
             :on-mouse-down (when-not animating? #(start-drag! pos))
             :on-mouse-enter #(drag-to! pos)
             :on-mouse-up end-drag!}])])]]))
