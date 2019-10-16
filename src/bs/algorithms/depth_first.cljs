(ns bs.algorithms.depth-first)

(defn- parents->path [parents target]
  (loop [cur target
         result (list target)]
    (if-let [parent (get parents cur)]
      (recur parent (conj result parent))
      result)))

(defn- *-first [start end neighbors-fn peek-fn pop-fn]
  (loop [stack [start]
         visited? #{}
         parents {}]
    (cond
      (empty? stack) nil
      (= end (peek-fn stack)) (parents->path parents end)
      :else
      (let [cur (peek-fn stack)
            visited? (set (conj visited? cur))
            neighbors (remove visited? (neighbors-fn cur))]
        (recur (vec (into (remove visited? (pop-fn stack)) neighbors))
               visited?
               (merge parents (zipmap neighbors (repeat cur)) ))))))

(defn depth-first
  "Traverses a graph via depth first. Accepts a visit-fn and a
  neighbors-fn to get the neighboring nodes given a certain node."
  [start end neighbors-fn]
  (*-first start end neighbors-fn first (partial drop 1)))

(defn breadth-first
  "Traverses a graph via depth first. Accepts a visit-fn and a
  neighbors-fn to get the neighboring nodes given a certain node."
  [start end neighbors-fn]
  (*-first start end neighbors-fn peek pop))

(comment
  (let [g {:a [:c :b]
           :b [:c :d]
           :c []
           :d []}]
    (breadth-first :a :d g))

  )
