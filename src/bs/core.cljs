(ns bs.core
  (:require [bs.db :as db]
            [bs.ui :as ui]
            [reagent.core :as r]))

(defonce db (r/atom (db/new-db)))

(defn root [db]
  [:<>
   (when-let [e (:db/error @db)]
     [:div.alert.w-full.text-center
      [:button.mdi.mdi-close.float-right.text-red-700.text-lg
       {:on-click #(db/hide-error! db)}]
      [:p e]])
   [:div.px-16.py-3.bg-blue-600.text-center.text-white.text-3xl.font-bold
    [:i.mdi.mdi-tent]
    [:h1.ml-3.inline-block "Boyscout"]]
   [:div.flex.mt-8.flex-wrap
    [:div.px-8 {:class "xl:w-1/4"}
     [ui/sidebar db]]
    [:div.px-3.mt-8.lg:mt-0 {:class "xl:w-3/4"}
     [ui/board-table db]]]])

(defn ^:dev/after-load render! []
  (r/render [root db] (.getElementById js/document "app")))

(def main! render!)
