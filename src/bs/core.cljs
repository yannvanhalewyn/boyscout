(ns bs.core
  (:require [bs.board :as board]
            [bs.algorithms :as alg]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(defn- new-db []
  (let [[src target] [[10 10] [30 15]]]
    {:db/board (-> (board/make 66 20)
                   (board/set-source src)
                   (board/set-target target))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(def SPEED 10)

(defn- animate* [state coll & {:keys [f offset interval]}]
  (doseq [[i el] (map-indexed vector coll)]
    (js/setTimeout
     #(swap! state update :db/board f el)
     (+ offset (* i interval)))))

(defn- animate! [state]
  (let [{:board/keys [source target] :as board} (:db/board @state)
        {::alg/keys [visitation-order shortest-path]} (alg/dijkstra board source target)]
    (if (empty? shortest-path)
      (do (swap! state assoc :db/error "Target is unreachable")
          (js/setTimeout #(swap! state dissoc :db/error) 2000))
      (do (animate* state visitation-order
                    :f board/mark-visited
                    :interval SPEED)
          (animate* state shortest-path
                    :f board/mark-path
                    :interval (* SPEED 4)
                    :offset (* (count visitation-order) SPEED))))))

(defn board-table [state]
  (let [st @state
        {:board/keys [width height] :as board} (:db/board st)]
    [:table
     [:tbody
      (for [y (range height)]
        ^{:key y}
        [:tr
         (for [x (range width)
               :let [pos [x y]]]
           ^{:key x}
           [:td.cell
            {:class (for [[f v] {board/source? "cell--source"
                                 board/target? "cell--target"
                                 board/path? "cell--path"
                                 board/visited? "cell--visited"
                                 board/wall? "cell--wall"}
                          :when (f board [x y])]
                      v)
             :on-mouse-down #(let [type (cond (board/source? board pos) :drag/source
                                              (board/target? board pos) :drag/target
                                              :else :drag/wall)]
                               (swap! state assoc :db/dragging type)
                               (when (= :drag/wall type)
                                 (swap! state update :db/board board/make-wall pos)))
             :on-mouse-enter #(when-let [f (case (:db/dragging st)
                                             :drag/source board/set-source
                                             :drag/target board/set-target
                                             :drag/wall board/make-wall nil)]
                                (swap! state update :db/board f pos))
             :on-mouse-up (when (:db/dragging st)
                            #(swap! state dissoc :db/dragging))}])])]]))

(defn root [state]
  [:<>
   [:h1 "Pathfinder visualizer"]
   [:button {:on-click #(animate! state)} "Visualize!"]
   [:button {:on-click #(reset! state (new-db))} "Reset"]
   (when-let [e (:db/error @state)]
     [:p.u-error e])
   [board-table state]])

(defn ^:dev/after-load render! []
  (r/render [root (r/atom (new-db))] (.getElementById js/document "app")))

(def main! render!)
