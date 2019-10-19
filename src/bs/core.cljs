(ns bs.core
  (:require [bs.db :as db]
            [bs.ui :as ui]
            [bs.utils :as u]
            [reagent.core :as r]))

(defonce db (r/atom (db/new-db)))

(defn root [db]
  [:<>
   (when-let [[error status] (:db/error @db)]
     [:div.alert.w-full.px-4.py-6.text-center
      {:class (when (= status :error/closing) "opacity-0")}
      [:button.mdi.mdi-close.text-lg.float-right.hover:text-red-900
       {:on-click #(db/hide-error! db)}]
      [:p error]])
   [:div.px-24.py-2.bg-gray-100.shadow
    [:a.pt2.btn.float-right.align-middle.text-gray-800.hover:bg-gray-200
     {:style {:line-height "34px"}
      :href "https://www.github.com/yannvanhalewyn/boyscout"
      :target "_blank"}
     [:span "View source on"]
     [:img.inline.ml-4 {:src (u/resource-path "img/github-mark-32.png")}]]
    [:span.text-blue-700.align-middle
     [:i.mdi.mdi-tent.text-4xl]
     [:h1.ml-3.inline-block.font-logo.font-bold.text-4xl.tracking-widest "Boyscout"]]
    [:span.ml-10.text-gray-600.align-middle
     "A pathfinding and "
     [:button.text-blue-600.cursor-default
      (when-not (db/animating? @db)
        {:class "cursor-pointer underline hover:text-blue-300"
         :on-click #(db/generate-maze! db)})
      "maze"]
     " generation visualizer"]]
   [:div.flex.mt-8.flex-wrap.max-w-7xl.m-auto
    [:div.px-8 {:class "xl:w-1/3"}
     [ui/sidebar db]]
    [:div.px-3.mt-8.lg:mt-0 {:class "xl:w-2/3"}
     [ui/board-table db]]]])

(defn ^:dev/after-load render! []
  (r/render [root db] (.getElementById js/document "app")))

(def main! render!)
