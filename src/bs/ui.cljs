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
   [:div.absolute.w-full.h-full.bg-gray-900.opacity-50
    {:on-click on-close}]
   [:div.bg-white.md:max-w-6xl.mx-auto.rounded.shadow-lg.z-50.overflow-y-auto
    [:h1.text-3xl.py-4.px-10.text-indigo-900.shadow-inner.shadow-2xl.bg-gray-200 "Pick algorithm"]
    [:div.py-4.text-left.px-6
     [:div.flex
      (for [{::alg/keys [key name description img-url] :as alg} alg/ALL
            :let [selected? (= key (::alg/key current-alg))]]
        ^{:key key}
        [:a.p-4.pb-12.m-2.relative.cursor-default.text-gray-900
         {:class (when selected? " bg-indigo-500 rounded")}
         [:img.float-right.pl-4.w-32.h-32 {:src img-url}]
         [:h1.text-xl.font-bold
          {:class (when selected? "text-indigo-100")}
          name]
         [:p.font-serif.mb-12.mt-2.leading-relaxed.tracking-wide
          {:class (if selected?
                    "text-indigo-200"
                    "text-gray-700")}
          description]
         (if selected?
           [:div.absolute.bottom-0.mb-4.py-2.-ml-12.border.rounded
            {:style {:left "50%"}}
            [:span.px-3.py-2.text-gray-200 "SELECTED"]]
           [:div.absolute.w-full.bottom-0.text-center
            [:button.mb-5.px-3.py-2.shadow-xl.shadow-inner.bg-indigo-600.tracking-wider.rounded.text-white.hover:bg-indigo-400
             {:on-click #(on-change alg)}
             "Select"]])])]
     [:button.float-right.px-4.bg-transparent.py-3.rounded-lg
      {:on-click on-close}
      "Close"]
     [:div.clearfix]]]])

(defn sidebar
  "A little side panel on the left with the algorithm name description
  and some facts. Can also open a modal and change the selected algorithm"
  []
  (let [modal? (r/atom false)]
    (fn [state]
      (let [{:db/keys [current-alg]} @state
            animating? (db/animating? @state)]
        [:div
         [:div.py-6.px-6.bg-teal-500.rounded

          ;; Header
          [:div.w-full.flex.justify-between.items-center
           [:h1.ml-1.inline-block.text-2xl.text-white
            (::alg/name current-alg)]
           (when-not (db/animating? @state)
             [:button.btn.text-sm.text-white.hover:bg-teal-400
              {:on-click #(reset! modal? true)} "Change"])]

          ;; Body
          [:div
           [:img.float-left.p-2.pl-0.mt-4.w-24.h-24 {:src (::alg/img-url current-alg)}]
           [:p.mt-4.font-serif.text-justify.text-teal-200.leading-relaxed
            (::alg/description current-alg)]]

          ;; Buttons footer
          [:div.text-center.h-full.mt-5
           (if animating?
             [:button.btn.btn--red.text-sm.mx-4
              {:on-click #(db/cancel-animation! state)}
              [:i.mdi.mdi-stop-circle-outline.animate-pulsing]
              [:span.pl-3.font-bold.text-base "Stop"]]
             [:div.mt-8
              [:button.btn.text-lg.bg-white.text-teal-600.font-bold.hover:bg-teal-600.hover:text-white
               {:on-click #(db/animate! state)}
               (str "Visualize " (or (::alg/short-name current-alg)
                                     (::alg/name current-alg)))]
              [:div.text-center.mt-1
               [:button.mx-4.text-white.underline.hover:no-underline
                {:on-click #(reset! state (db/new-db))
                 :class (when (or animating? (= (:db/board @state)
                                                (:db/board (db/new-db))))
                          "opacity-0")}
                "reset"]]])]]

         ;; Checkmarks
         [:div.mx-12.py-2.bg-teal-100.rounded-lg.relative.shadow-lg
          {:style {:left 180 :top -40}}
          (for [check (checkmarks current-alg)]
            ^{:key (:check/title check)}
            [:div.my-3.px-4.text-base.text-center
             [:i.mdi.text-lg.mr-1
              {:class (if (:check/checked? check)
                        "mdi-check text-green-500"
                        "mdi-close text-red-500")}]
             [:span.font-sans.text-gray-900.font-semibold.tracking-wider (:check/title check)]
             [:p.pl-1.mt-1.text-left.text-base.text-teal-700 (:check/body check)]])]
         (when @modal?
           [select-algorithm-modal {:on-close #(reset! modal? false)
                                    :on-change #(do (reset! modal? false)
                                                    (db/update! state assoc :db/current-alg %))
                                    :current-alg current-alg}])]))))

(defn board-table
  "The main animated attraction"
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
