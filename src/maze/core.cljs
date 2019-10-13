(ns maze.core
  (:require [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn root [props]
  (.log js/console "root")
  [:div "Hello!"])

(defn render! []
  (.log js/console (.getElementById js/document "app"))
  (r/render [root] (.getElementById js/document "app")))

(defn main! []
  (.log js/console "MAIN")
  (render!))

(.log js/console "OK?")
