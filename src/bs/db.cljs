(ns bs.db
  (:require [bs.algorithm :as alg]
            [bs.animation :as animation]
            [bs.board :as board]))

(def ANIMATION_SPEED 8)

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

(defn- animate!* [db {::alg/keys [path visitation-order] :as alg-result}]
  (let [mk-step #(animation/make-step (board/cell-id %1) %2 (* %3 ANIMATION_SPEED))]
    (animation/start!
     (concat (map #(mk-step % "cell--visited-animated" 1) visitation-order)
             (map #(mk-step % "cell--path-animated" 4) path))
     #(swap! db assoc :db/animation %
             :db/alg-result alg-result))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn new-db []
  (let [[src target] [[10 10] [30 15]]]
    {:db/board (-> (board/make 56 20)
                   (board/set-source src)
                   (board/set-target target))
     :db/current-alg (first alg/ALL)}))

(defn hide-error! [db]
  (swap! db dissoc :db/error))

(defn show-error! [db err]
  (swap! db assoc :db/error err)
  (js/setTimeout #(hide-error! db) 5000))

(defn update!
  "A middleware like way to update the app-db. If the algorithm or
  the board changes, recalculate the algorithm"
  [db f & args]
  (let [old-db @db
        new-db (apply f @db args)]
    (if (recalculate-alg? old-db new-db)
      (reset! db (assoc new-db :db/alg-result (process-alg new-db)))
      (reset! db new-db))))

(defn animate!
  "Calculates the algorithm result of the current board and selected
  algorithm, and kicks-off an animation process."
  [db]
  (let [{::alg/keys [path] :as result} (process-alg @db)]
    (if (empty? path)
      (show-error! db "Target is unreachable")
      (swap! db assoc :db/animation
             (animate!* db result)))))

(defn animating? [db]
  (animation/running? (:db/animation db)))

(defn cancel-animation! [db]
  (swap! db update :db/animation animation/cancel!))
