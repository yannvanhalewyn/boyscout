(ns bs.db
  (:require [bs.algorithm :as alg]
            [bs.animation :as animation]
            [bs.board :as board]
            [bs.maze :as maze]))

(def SPEEDS
  [#:speed {:name "Slow" :path 200 :visit 300}
   #:speed {:name "Fast" :path 40  :visit 7}])

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
  [db steps db-before db-after & [done-timeout]]
  (let [done-fn #(reset! db (assoc db-after :db/animation %))]
    (reset! db (assoc db-before :db/animation
                      (bs.animation/start! steps done-fn done-timeout)))))

(defn- animation-steps [{::alg/keys [visitation-order path]} animation-speed]
  (concat
   (map #(animation/make-step (board/cell-id %) "cell--visited-animated"
                              (:speed/visit animation-speed) "cell--path")
        visitation-order)
   (map #(animation/make-step (board/cell-id %) "cell--path-animated"
                              (:speed/path animation-speed) "cell--source")
        path)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn new-db []
  {:db/board (-> (board/make 45 30)
                 (board/set-source [15 10])
                 (board/set-target [30 20]))
   :db/current-alg (first alg/ALL)
   :db/animation-speed (second SPEEDS)
   :db/tool (:tool/key (first TOOLS))})

(defn reset-board! [db]
  (swap! db #(assoc (dissoc % :db/alg-result)
               :db/board (:db/board (new-db)))))

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
      (run-animation! db (animation-steps result animation-speed)
                      db* (assoc db* :db/alg-result result) 1000))))

(defn generate-maze!
  "Generates a maze, clears the board's walls and kicks-off a maze animation"
  [db]
  (let [{:db/keys [board animation-speed] :as db*} @db
        {:board/keys [width height source target]} board
        walls (remove #{source target} (maze/recursive-division width height))
        steps (for [w walls]
                (bs.animation/make-step (board/cell-id w) "cell--wall-animated"
                                        (:speed/visit animation-speed)))
        empty-board (board/reset-edges board)
        db-before (assoc db* :db/board empty-board)
        db-after (assoc (dissoc db* :db/alg-result)
                   :db/board (reduce board/make-wall empty-board walls))]
    (run-animation! db steps db-before db-after)))

(defn animating? [db]
  (animation/running? (:db/animation db)))

(defn cancel-animation! [db]
  (swap! db update :db/animation animation/cancel!))
