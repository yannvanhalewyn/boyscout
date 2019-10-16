(ns bs.animation
  (:require [bs.utils :as u]
            [clojure.core.async :as a :refer [go-loop]]))

(defn- rewind!
  "If the animation is cancelled, go ahead and remove all those
  classes we added."
  [steps]
  (doseq [{::keys [id class]} steps]
    (u/remove-class! id class)))

(defn make-step
  "Makes a step that can be used for an animation"
  [id class timeout]
  {::id id
   ::class class
   ::timeout timeout})

(defn start! [steps done-fn]
  (let [close-ch (a/chan)]
    (go-loop [[{::keys [id class timeout]} & others] steps]
      (u/add-class! id class)
      (if (seq others)
        (let [[v _] (a/alts! [(a/timeout timeout) close-ch])]
          (if (= v :animation/stop!)
            (rewind! (take (- (count steps) (count others)) steps))
            (recur others)))
        (done-fn {::status ::done})))
    {::close-ch close-ch
     ::status ::running}))

(defn cancel! [{::keys [close-ch]}]
  (a/put! close-ch :animation/stop!)
  {::status ::cancelled})

(defn running? [{::keys [status]}]
  (= status ::running))