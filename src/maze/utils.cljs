(ns maze.utils)

(defn split-by
  "Returns a vector with two elements: the first is the collection of
  elements for which the predicate returned a truthy value, the second
  for the others."
  [pred coll]
  (let [{survivors true others false} (group-by (comp boolean pred) coll)]
    [survivors others]))
