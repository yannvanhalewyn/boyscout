(ns bs.core
  (:require [reagent.core :as r]))

(defn new-db []
  {})

(defn root [db]
  [:div.p-4 "I don't do much (yet)"])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
