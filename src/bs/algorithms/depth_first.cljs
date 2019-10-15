(ns bs.algorithms.depth-first)

(defn depth-first [start neighbors-fn]
  (loop [stack [start]
         visited []]
    (if (empty? stack)
      visited
      (let [cur (peek stack)
            neighbors (neighbors-fn cur)
            visited (conj visited cur)]
        (if (= :depth-first/done neighbors)
          visited
          (recur (into (pop stack) (remove (set visited) neighbors)) visited))))))
