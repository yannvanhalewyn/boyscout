(ns bs.ui
  (:require [bs.board :as board]))

(defn board-table
  "An interactive html table displaying all board cells"
  [db]
  (let [{:db/keys [board]} @db
        {:board/keys [width height] :as board} board]
    [:table.shadow-lg.border-2.border-blue-100
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
                          :when (f board pos)] v)}])])]]))
