(ns bs.ui
  (:require [bs.algorithm :as alg]
            [bs.board :as board]
            [bs.db :as db]
            [bs.utils :as u]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Animated Board

(def cells-cache
  ^{:doc "For react performance, don't swap in every wall while
          dragging, but rather natively animate them, store them in a
          cache and flush them to the app db on mouse up."}
  (atom '()))

(defn draw-handler
  "Makes a new handler for drawing (forests and walls)"
  [board-fn cell-fn]
  {:drag/start #(db/update! %1 update :db/board board-fn %2)
   :drag/move (fn [board pos]
                (cell-fn board pos)
                (swap! cells-cache conj pos))
   :drag/end (fn [db]
               (when-let [cells (seq @cells-cache)]
                 (reset! cells-cache '())
                 (db/update! db update :db/board #(reduce board-fn % cells))))})

(def drag-handlers
  ^{:doc "A drag handler has three functions: start, move and end. For
          each of the drag types there is a drag handler that
          implements one or more of these."}
  (let [add-class #(fn [_ pos] (u/add-class! (board/cell-id pos) %))
        remove-class #(fn [_ pos] (u/remove-class! (board/cell-id pos) %))]
    {:drag/source {:drag/move #(db/update! %1 update :db/board board/set-source %2)}
     :drag/target {:drag/move #(db/update! %1 update :db/board board/set-target %2)}
     :drag/make-wall (draw-handler board/make-wall (add-class "cell--wall-animated"))
     :drag/clear-wall (draw-handler board/destroy-wall (remove-class "cell--wall"))
     :drag/clear-forest (draw-handler board/unilever (remove-class "cell--forest"))
     :drag/make-forest (draw-handler
                        board/make-forest
                        #(let [f (add-class "cell--forest-animated")
                               {:board/keys [source target] :as b} (:db/board @%1)]
                           (when-not (or (board/wall? b %2) (= source %2) (= target %2))
                             (f %1 %2))))}))

(defn- drag-type [{:db/keys [board] :as db} pos]
  (cond (board/source? board pos) :drag/source
        (board/target? board pos) :drag/target
        (board/wall? board pos)   :drag/clear-wall
        (board/forest? board pos) :drag/clear-forest
        :else (if (= (db/current-tool db) :tool/forest)
                :drag/make-forest
                :drag/make-wall)))

(defn board-table
  "The main animated attraction"
  [db]
  (let [{:db/keys [board alg-result drag-target current-alg] :as state} @db
        {:board/keys [width height] :as board} board
        animating? (db/animating? state)
        {::alg/keys [path visitation-order]} (when-not animating? alg-result)
        path? (let [s (set path)] #(contains? s %2))
        visited? (let [s (set visitation-order)] #(contains? s %2))
        drag-handler (get drag-handlers drag-target)
        start-drag! (fn [pos]
                      (let [type (drag-type state pos)]
                        (db/update! db assoc :db/drag-target (drag-type state pos))
                        ((get-in drag-handlers [type :drag/start] u/no-op) db pos)))
        end-drag!   (fn []
                      (db/update! db dissoc :db/drag-target)
                      ((:drag/end drag-handler u/no-op) db))]
    [:table.shadow-lg.border-2.border-blue-100 {:on-mouse-leave end-drag!}
     [:tbody
      (for [y (range height)]
        ^{:key y}
        [:tr
         (for [x (range width) :let [pos [x y]]]
           ^{:key x}
           [:td.cell
            {:id (board/cell-id pos)
             :class (for [[f v] {board/source? "cell--source"
                                 board/target? "cell--target"
                                 board/wall? "cell--wall"
                                 board/forest? (when (::alg/weighted? current-alg) "cell--forest")
                                 path? "cell--path"
                                 visited? "cell--visited"}
                          :when (f board pos)] v)
             :style (when animating? {:cursor "wait"})
             :on-mouse-down (when-not animating? #(start-drag! pos))
             :on-mouse-enter #((:drag/move drag-handler u/no-op) db pos)
             :on-mouse-up end-drag!}])])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Toolbar

(defn toolbar [db]
  (let [{:db/keys [animation-speed current-alg] :as db*} @db]
    [:div.flex.items-center.justify-between.mb-4.pb-3.border-b-2.border-gray-200
     [:div
      (for [{:tool/keys [key icon-class name]} db/TOOLS
            :let [disabled? (and (= key :tool/forest) (not (::alg/weighted? current-alg)))]]
        ^{:key key}
        [:button.px-4.py-1.tracking-wider
         {:class
          (str (str (if (= (db/current-tool db*) key)
                      "text-gray-700 font-bold border rounded-lg cursor-default"
                      "text-gray-600 hover:text-gray-700 cursor-pointer")
                    (when disabled? " cursor-not-allowed")))
          :on-click #(swap! db assoc :db/tool key)}

         [:i.inline-block.cell.w-4.h-4.rounded.align-middle.cursor-inherit
          {:class (str icon-class (when disabled? " opacity-50"))}]
         [:span.ml-3
          {:class (when disabled? "text-gray-500")}
          name]])]
     [:div
      (for [{:speed/keys [name] :as cur} (vals db/SPEEDS)]
        ^{:key name}
        [:button.mr-3.px-1.text-gray-500
         {:class (if (= animation-speed cur)
                   "text-blue-600 font-bold border-b-2 border-blue-600 cursor-default"
                   "text-gray-500 hover:text-gray-600")
          :on-click #(db/change-animation-speed! db cur)}
         name])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sidebar and modal

(defn- checkmarks [{::alg/keys [weighted? shortest-path?]}]
  [(if weighted?
     {:check/checked? true
      :check/title "Weighted"
      :check/body (str "A weighted algorithm means that edges in the graph can "
                       "be more costly to traverse than others.")}
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

(defn- modal-checkmarks [alg text-color]
  [:<>
   (for [check (checkmarks alg)]
     ^{:key (:check/title check)}
     [:div.ml-1
      [:i.mdi.text-xl.align-middle
       {:class (if (:check/checked? check)
                 ["mdi-check" "text-green-400"]
                 ["mdi-close" "text-red-600"])}]
      [:span.ml-2 {:class text-color} (:check/title check)]])])

(defn- select-algorithm-modal [{:keys [on-close on-change current-alg]}]
  [:div.modal.z-50.fixed.w-full.h-full.top-0.left-0.flex.items-center.justify-center

   ;; Background click
   [:div.absolute.w-full.h-full.bg-gray-900.opacity-50
    {:on-click on-close}]
   [:div.bg-white.max-w-7xl.mx-auto.rounded.shadow-lg.z-50.overflow-y-auto

    ;; Header
    [:h1.text-3xl.py-4.px-10.text-teal-900.shadow.bg-gray-200 "Pick algorithm"]

    [:div.py-4.text-left.px-6
     [:div.flex
      (for [{::alg/keys [key name description img-url] :as alg} alg/ALL
            :let [selected? (= key (::alg/key current-alg))]]

        ;; Algorithm option
        ^{:key key}
        [:div.w-72.p-4.pb-12.m-2.relative.cursor-default.text-gray-900
         {:class (when selected? " bg-teal-500 rounded")}
         [:img.float-right.pl-4.w-32.h-32 {:src img-url}]
         [:h1.text-xl.font-bold
          {:class (when selected? "text-teal-100")}
          name]
         [:p.font-serif.mb-24.mt-2.leading-relaxed.tracking-wide
          {:class (if selected?
                    "text-teal-200"
                    "text-gray-700")}
          description]

         ;; Checkmarks and select(ed) button
         [:div.absolute.w-full.bottom-0.left-0
          [:div.relative.bottom-0.left-0.flex.items-center.justify-between.mx-3.border-t-4.border-gray-200
           (if selected?
             [:<>
              [:div.my-6 [modal-checkmarks alg "text-white"]]
              [:div.py-1.px-2.text-sm.border.rounded.text-gray-200 [:span "SELECTED"]]]
             [:<>
              [:div.my-6 [modal-checkmarks alg "text-gray-800"]]
              [:button.px-3.py-2.shadow-xl.shadow-inner.bg-teal-600.tracking-wider.rounded.text-white.hover:bg-teal-400
               {:on-click #(on-change alg)}
               "Select"]])]]])]

     [:button.float-right.px-4.bg-transparent.py-3.rounded-lg.text-gray-700.hover:text-gray-500
      {:on-click on-close}
      "Close"]
     [:div.clearfix]]]])

(defn sidebar
  "The side panel on the left with the algorithm name description,
  facts and animation state. and some facts. Can also open a modal and
  change the selected algorithm"
  []
  (let [modal? (r/atom false)]
    (fn [db]
      (let [{:db/keys [current-alg]} @db
            animating? (db/animating? @db)]
        [:div
         [:div.relative.py-6.px-6.h-110.bg-teal-500.rounded

          ;; Header
          [:div.w-full.flex.justify-between.items-center
           [:div.ml-1
            [:h1.text-3xl.text-white
             (::alg/name current-alg)]
            [:span
             [:a.cursor-pointer.underline.text-teal-300.hover:text-teal-100

              {:href (::alg/source-url current-alg) :target "_blank"}
              "View code"]
             [:img.inline.ml-2.opacity-75 {:src (u/resource-path "img/github-mark-32.png")
                                           :width 16
                                           :height 16}]]]
           (when-not (db/animating? @db)
             [:button.btn.text-sm.text-white.hover:bg-teal-400
              {:on-click #(reset! modal? true)} "Change"])]

          ;; Body
          [:div
           [:img.float-left.p-2.pl-0.mt-4.w-24.h-24 {:src (::alg/img-url current-alg)}]
           [:p.mt-4.font-serif.text-justify.text-teal-200.leading-loose
            (::alg/description current-alg) " "
            [:a.cursor-pointer.underline.text-teal-300.hover:text-teal-100
             {:href (::alg/wiki-url current-alg) :target "_blank"}
             "Learn more"]]]

          ;; Buttons footer
          [:div.absolute.bottom-0.left-0.w-full.mb-8.text-center
           (if animating?
             [:button.btn.btn--red.text-sm
              {:on-click #(db/cancel-animation! db)}
              [:i.mdi.mdi-stop-circle-outline.animate-pulsing]
              [:span.pl-3.font-bold.text-base "Stop"]]
             [:button.btn.text-lg.bg-white.text-teal-600.font-bold.hover:bg-teal-600.hover:text-white
              {:on-click #(db/animate-alg! db)}
              (str "Run " (or (::alg/short-name current-alg)
                              (::alg/name current-alg)))
              [:i.mdi.mdi-graph.ml-2]])
           [:div.text-center.mt-1
            [:button.mx-4.text-white.underline.hover:no-underline
             {:on-click #(db/reset-board! db)
              :class (when (or animating?
                               (and (not (:db/alg-result @db))
                                    (= (:db/board @db)
                                       (:db/board (db/new-db)))))
                       "opacity-0")}
             "reset"]]]]

         ;; Checkmarks
         [:div.relative.mx-12.-mt-4.py-2.bg-teal-100.rounded-lg.shadow-lg
          (for [check (checkmarks current-alg)]
            ^{:key (:check/title check)}
            [:div.my-3.px-4.text-base.text-center
             [:i.mdi.text-lg.mr-2
              {:class (if (:check/checked? check)
                        "mdi-check text-green-500"
                        "mdi-close text-red-500")}]
             [:span.font-sans.text-gray-900.font-semibold.tracking-wider (:check/title check)]
             [:p.pl-1.mt-1.text-left.text-base.text-teal-700 (:check/body check)]])]
         (when @modal?
           [select-algorithm-modal {:on-close #(reset! modal? false)
                                    :on-change #(do (reset! modal? false)
                                                    (db/update! db assoc :db/current-alg %))
                                    :current-alg current-alg}])]))))
