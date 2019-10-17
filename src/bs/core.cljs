(ns bs.core
  (:require [bs.db :as db]
            [bs.ui :as ui]
            [reagent.core :as r]))

(defn root [state]
  (let [animating? (db/animating? @state)]
    [:<>
     [:div.header
      [:span.logo-title.mr-2]
      [:h1.text-3xl.inline-block.text-white.mr-8 "Boyscout"]
      (if animating?
        [:button.btn.btn--red.mx-4
         {:on-click #(db/cancel-animation! state)}
         [:i.mdi.mdi-stop-circle-outline.animate-pulsing]
         [:span.pl-3.font-bold.text-base "Stop"]]
        [:button.btn.mx-4.text-white.hover:bg-white.hover:text-blue-700
         {:on-click #(db/animate! state)} "Visualize!"])
      (when-not animating?
        [:button.mx-4.text-white.underline.hover:no-underline
         {:on-click #(reset! state (db/new-db))} "Reset"])]
     (when-let [e (:db/error @state)]
       [:div.alert [:p e]])
     [:div.flex.mt-8.flex-wrap
      [:div.px-8 {:class "xl:w-1/4"}
       [ui/algorithm-summary state]]
      [:div.px-3.mt-8.lg:mt-0 {:class "xl:w-3/4"}
       [ui/board-table state]]]]))

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (db/new-db))] (.getElementById js/document "app")))

(def main! render!)
