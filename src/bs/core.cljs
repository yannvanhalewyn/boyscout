(ns bs.core
  (:require [bs.db :as db]
            [bs.ui :as ui]
            [reagent.core :as r]))

(defn root [state]
  [:<>
   (when-let [e (:db/error @state)]
     [:div.alert.w-full.text-center
      [:button.mdi.mdi-close.float-right.text-red-700.text-lg
       {:on-click #(swap! state dissoc :db/error)}]
      [:p e]])
   [:div.px-16.py-3.bg-blue-600.text-center.text-white.text-3xl.font-bold
    [:i.mdi.mdi-tent]
    [:h1.ml-3.inline-block "Boyscout"]]
   [:div.flex.mt-8.flex-wrap
    [:div.px-8 {:class "xl:w-1/4"}
     [ui/sidebar state]]
    [:div.px-3.mt-8.lg:mt-0 {:class "xl:w-3/4"}
     [ui/board-table state]]]])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (db/new-db))] (.getElementById js/document "app")))

(def main! render!)
