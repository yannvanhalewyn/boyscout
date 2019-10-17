(ns bs.db
  (:require [bs.algorithm :as alg]
            [bs.animation :as animation]
            [bs.board :as board]))

(def ANIMATION_SPEED 8)

(defn- recalculate-alg?
  "Wether or not the algorithm output should be recalculated. This
  should happen when there has been an animation and if either the
  board or selected algorithm have changed."
  [old-state new-state]
  (and (contains? old-state :db/alg-result)
       (or (not (identical? (:db/board old-state)
                            (:db/board new-state)))
           (not (identical? (:db/current-alg old-state)
                            (:db/current-alg new-state))))))

(defn- process-alg
  "Takes the current algorithm and the current board from the state
  and processes the current algorithm on it."
  [{:db/keys [current-alg board]}]
  (alg/process (::alg/key current-alg) board))

(defn- animate!* [state {::alg/keys [path visitation-order] :as alg-result}]
  (let [mk-step #(animation/make-step (board/cell-id %1) %2 (* %3 ANIMATION_SPEED))]
    (animation/start!
     (concat (map #(mk-step % "cell--visited-animated" 1) visitation-order)
             (map #(mk-step % "cell--path-animated" 4) path))
     #(swap! state assoc :db/animation %
             :db/alg-result alg-result))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn new-db []
  (let [[src target] [[10 10] [30 15]]]
    {:db/board (-> (board/make 56 20)
                   (board/set-source src)
                   (board/set-target target))
     :db/current-alg (first alg/ALL)}))

(defn hide-error! [state]
  (swap! state dissoc :db/error))

(defn show-error! [state err]
  (swap! state assoc :db/error err)
  (js/setTimeout #(hide-error! state) 5000))

(defn update!
  "A middleware like way to update the app-state. If the algorithm or
  the board changes, recalculate the algorithm"
  [state f & args]
  (let [old-state @state
        new-state (apply f @state args)]
    (if (recalculate-alg? old-state new-state)
      (reset! state (assoc new-state :db/alg-result (process-alg new-state)))
      (reset! state new-state))))

(defn animate!
  "Calculates the algorithm result of the current board and selected
  algorithm, and kicks-off an animation process."
  [state]
  (let [{::alg/keys [path] :as result} (process-alg @state)]
    (if (empty? path)
      (show-error! state "Target is unreachable")
      (swap! state assoc :db/animation
             (animate!* state result)))))

(defn animating? [state]
  (animation/running? (:db/animation state)))

(defn cancel-animation! [state]
  (swap! state update :db/animation animation/cancel!))
