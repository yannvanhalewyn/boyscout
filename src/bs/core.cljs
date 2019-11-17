(ns bs.core
  (:require [reagent.core :as r]))

(defonce db (r/atom {}))

(defn root [db]
  [:div "I don't do much, yet"])

(defn ^:dev/after-load render! []
  (r/render [root db] (.getElementById js/document "app")))

(def main! render!)
