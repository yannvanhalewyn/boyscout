(ns bs.core
  (:require [bs.db :as db]
            [bs.ui :as ui]
            [reagent.core :as r]))

(defonce db (r/atom (db/new-db)))

(defn root [db]
  [:<>
   (let [error (:db/error @db)]
     [:div.alert.w-full.px-4.py-6.text-center {:class (when error "alert--is-open")}
      [:button.mdi.mdi-close.text-lg.float-right.hover:text-red-900
       {:on-click #(db/hide-error! db)}]
      [:p error]])
   [:div.px-16.py-3.bg-blue-600.text-center.text-white.text-3xl.font-bold.shadow-md
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
