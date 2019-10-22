(ns bs.core
  (:require [bs.db :as db]
            [bs.ui :as ui]
            [bs.utils :as u]
            [reagent.core :as r]))

(defonce db (r/atom (db/new-db)))

(defn root [db]
  [:<>
   (when-let [[error status] (:db/error @db)]
     [:div.alert.w-full.px-4.py-6.text-center.z-30
      {:class (when (= status :error/closing) "opacity-0")}
      [:button.mdi.mdi-close.text-lg.float-right.hover:text-red-900
       {:on-click #(db/hide-error! db)}]
      [:p error]])
   [:div.py-8
    [:div.flex.flex-wrap.max-w-7xl.m-auto
     [:div.px-8 {:class "lg:w-2/5 xl:w-1/3"}
      [ui/sidebar db]]
     [:div.px-3 {:class "lg:w-3/5 xl:w-2/3"}
      [ui/toolbar db]
      [ui/board-table db]]]]
   [:div.right-0.py-3.mr-20
    [:a.float-right.px-4.pb-1.pt-2.font-semibold.rounded.tracking-wider.shadow.align-middle.text-gray-800.hover:bg-white
     {:style {:line-height "34px"}
      :href "https://www.github.com/yannvanhalewyn/boyscout"
      :target "_blank"}
     [:span.text-sm "View source on"]
     [:img.inline.ml-4 {:src (u/resource-path "img/github-mark-32.png")}]]]])

(defn ^:dev/after-load render! []
  (r/render [root db] (.getElementById js/document "app")))

(def main! render!)
