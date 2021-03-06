(ns bs.animation
  (:require [bs.utils :as u]
            [clojure.core.async :as a :refer [go-loop]]))

(def REWIND_SPEED 2)

(defn make-step
  "Makes a step that can be used for an animation"
  [id class & [duration visit-class]]
  {::id id
   ::class class
   ::visit-class visit-class
   ::duration (or duration 1)})

(defn start!
  "Takes a list of animation steps and animates them in order. Will
  take a timeout between every step of speed * step-duration.
  Immediately an animation object is returned,which can be used to
  cancel the running animation. When all the steps are completed, will
  call the done function with a new 'done' animation
  object. Optionally, a done-timeout can be passed in to wait for some
  visual side-effects to be over before calling the done
  function. This timeout should probably be equal to the duration of
  the last animation step."
  [steps speed-in-ms done-fn & [done-timeout]]
  (let [change-ch (a/chan)]
    (go-loop [[{::keys [id class duration visit-class]} & others] steps
              speed-in-ms speed-in-ms]
      (when visit-class (u/add-class! id visit-class))
      (u/add-class! id class)
      (if (seq others)
        (let [[v _] (a/alts! [(a/timeout (* duration speed-in-ms)) change-ch])]
          (when visit-class (u/remove-class! id visit-class))
          (case (first v)
            :animation/stop!
            (doseq [{::keys [id class]}
                    (reverse (take (- (count steps) (count others)) steps))]
              (a/<! (a/timeout REWIND_SPEED))
              (u/remove-class! id class))
            :animation/speed
            (recur others (second v))
            (recur others speed-in-ms)))
        (do (when done-timeout (a/<! (a/timeout done-timeout)))
            (done-fn {::status ::done}))))
    {::change-ch change-ch
     ::status ::running}))

(defn cancel! [{::keys [change-ch]}]
  (a/put! change-ch [:animation/stop!])
  {::status ::cancelled})

(defn set-speed! [{::keys [change-ch]} speed-in-ms]
  (when change-ch
    (a/put! change-ch [:animation/speed speed-in-ms])))

(defn running? [{::keys [status]}]
  (= status ::running))
