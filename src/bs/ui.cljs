(ns bs.ui
  (:require [bs.board :as board]
            [bs.utils :as u]))

(def cells-cache
  ^{:doc "For react performance, don't swap in every wall while
          dragging, but rather natively animate them, store them in a
          cache and flush them to the app db on mouse up."}
  (atom '()))

(defn draw-handler
  "Makes a new handler for drawing walls"
  [board-fn cell-fn]
  {:drag/start #(swap! %1 update :db/board board-fn %2)
   :drag/move (fn [board pos]
                (cell-fn board pos)
                (swap! cells-cache conj pos))
   :drag/end (fn [db]
               (when-let [cells (seq @cells-cache)]
                 (reset! cells-cache '())
                 (swap! db update :db/board #(reduce board-fn % cells))))})

(def drag-handlers
  ^{:doc "A drag handler has three functions: start, move and end. For
          each of the drag types there is a drag handler that
          implements one or more of these."}
  (let [add-class #(fn [_ pos] (u/add-class! (board/cell-id pos) %))
        remove-class #(fn [_ pos] (u/remove-class! (board/cell-id pos) %))]
    {:drag/source {:drag/move #(swap! %1 update :db/board board/set-source %2)}
     :drag/target {:drag/move #(swap! %1 update :db/board board/set-target %2)}
     :drag/make-wall (draw-handler board/make-wall (add-class "cell--wall-animated"))
     :drag/clear-wall (draw-handler board/destroy-wall (remove-class "cell--wall"))}))

(defn- drag-type [{:db/keys [board]} pos]
  (cond (board/source? board pos) :drag/source
        (board/target? board pos) :drag/target
        (board/wall? board pos)   :drag/clear-wall
        :else :drag/make-wall))

(defn board-table
  "An interactive html table displaying all board cells"
  [db]
  (let [{:db/keys [board] :as state} @db
        {:board/keys [width height] :as board} board
        drag-handler (get drag-handlers (:db/drag-type state))
        start-drag! (fn [pos]
                      (let [type (drag-type state pos)]
                        (swap! db assoc :db/drag-type (drag-type state pos))
                        ((get-in drag-handlers [type :drag/start] u/no-op) db pos)))
        end-drag!   (fn []
                      (swap! db dissoc :db/drag-type)
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
                                 board/wall? "cell--wall"}
                          :when (f board pos)] v)
             :on-mouse-down #(start-drag! pos)
             :on-mouse-enter #((:drag/move drag-handler u/no-op) db pos)
             :on-mouse-up end-drag!}])])]]))
