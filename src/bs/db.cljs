(ns bs.db
  (:require [bs.algorithm :as alg]
            [bs.animation :as animation]
            [bs.board :as board]
            [bs.maze :as maze]
            [bs.utils :as u]))

(def SPEEDS
  {:speed/slow   {:speed/name "Slow"   :speed/ms 300}
   :speed/medium {:speed/name "Medium" :speed/ms 130}
   :speed/fast   {:speed/name "Fast"   :speed/ms 8}})

(def TOOLS
  [#:tool {:key :tool/wall   :name "Walls"  :icon-class "cell--wall"}
   #:tool {:key :tool/forest :name "Forest" :icon-class "cell--forest"}])

(defn- recalculate-alg?
  "Wether or not the algorithm output should be recalculated. This
  should happen when there has been an animation and if either the
  board or selected algorithm have changed."
  [old-db new-db]
  (and (contains? old-db :db/alg-result)
       (or (not (identical? (:db/board old-db)
                            (:db/board new-db)))
           (not (identical? (:db/current-alg old-db)
                            (:db/current-alg new-db))))))

(defn- process-alg
  "Takes the current algorithm and the current board from the db
  and processes the current algorithm on it."
  [{:db/keys [current-alg board]}]
  (alg/process (::alg/key current-alg) board))

(defn- run-animation!
  [db steps speed & {:keys [db-before db-after-fn done-timeout]}]
  (let [done-fn #(reset! db (assoc (db-after-fn @db) :db/animation %))
        animation (bs.animation/start! steps speed done-fn done-timeout)]
    (reset! db (assoc db-before :db/animation animation))))

(defn- animation-steps [{::alg/keys [visitation-order path]}]
  (concat
   (map #(animation/make-step (board/cell-id %) "cell--visited-animated"
                              1 "cell--animation-current")
        visitation-order)
   (map #(animation/make-step (board/cell-id %) "cell--path-animated"
                              4 "cell--source")
        path)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn new-board []
  (let [[width height] (u/window-dimensions)
        [w h] (u/vmin [40 27] (map #(int (/ % 30)) [width height]))]
    (-> (board/make w h)
        (board/set-source (map int (u/v* [w h] [(/ 2 5) (/ 2 5)])))
        (board/set-target (map int (u/v* [w h] [(/ 3 5) (/ 3 5)]))))))

(defn new-db []
  {:db/board (new-board)
   :db/current-alg (first alg/ALL)
   :db/animation-speed (:speed/fast SPEEDS)
   :db/tool (:tool/key (first TOOLS))})

(defn reset-board! [db]
  (swap! db #(-> % (dissoc :db/alg-result) (assoc :db/board (new-board)))))

(defn hide-error! [db]
  (swap! db assoc :db/error [(first (:db/error @db)) :error/closing])
  (js/setTimeout #(swap! db dissoc :db/error) 300))

(defn show-error! [db err]
  (swap! db assoc :db/error [err :error/open])
  (js/setTimeout #(hide-error! db) 3000))

(defn update!
  "A middleware like way to update the app-db. If the algorithm or
  the board changes, recalculate the algorithm"
  [db f & args]
  (let [old-db @db
        new-db (apply f @db args)]
    (if (recalculate-alg? old-db new-db)
      (reset! db (assoc new-db :db/alg-result (process-alg new-db)))
      (reset! db new-db))))

(defn animate-alg!
  "Calculates the algorithm result of the current board and selected
  algorithm, and kicks-off an animation process."
  [db]
  (let [{:db/keys [animation-speed] :as db*} @db
        result (process-alg db*)]
    (if (empty? (::alg/path result))
      (show-error! db "Target is unreachable")
      (run-animation! db (animation-steps result) (:speed/ms animation-speed)
                      :db-before db*
                      :db-after-fn #(assoc % :db/alg-result result)
                      :done-timeout 1000))))

(defn generate-maze!
  "Generates a maze, clears the board's walls and kicks-off a maze animation"
  [db]
  (let [{:db/keys [board animation-speed] :as db*} @db
        {:board/keys [width height]} board
        board (-> board
                  (board/set-source [0 (-> height (- 2) rand-int inc)])
                  (board/set-target [(dec width) (rand-int height)]))
        {:board/keys [source target]} board
        walls (remove #{source target} (maze/recursive-division width height))
        steps (for [w walls]
                (bs.animation/make-step (board/cell-id w) "cell--wall-animated"))
        empty-board (board/reset-edges board)]
    (run-animation! db steps (:speed/ms animation-speed)
                    :db-before (assoc db* :db/board empty-board)
                    :db-after-fn #(assoc (dissoc % :db/alg-result)
                                    :db/board (reduce board/make-wall empty-board walls)))))

(defn animating? [db]
  (animation/running? (:db/animation db)))

(defn cancel-animation! [db]
  (swap! db update :db/animation animation/cancel!))

(defn change-animation-speed! [db speed]
  (animation/set-speed! (:db/animation @db) (:speed/ms speed))
  (swap! db assoc :db/animation-speed speed))

(defn current-tool [{:db/keys [current-alg tool]}]
  (if (::alg/weighted? current-alg) tool :tool/wall))
