(ns bs.core
  (:require [bs.board :as board]
            [bs.ui :as ui]
            [reagent.core :as r]))

(defn new-db []
  {:db/board (-> (board/make 30 20)
                 (board/set-source [10 5])
                 (board/set-target [20 15]))})

(defn root [db]
  [ui/board-table db])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
