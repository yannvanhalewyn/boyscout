(ns bs.utils)

(defn add-class! [id class]
  (.. js/document (getElementById id) -classList (add class)))

(defn remove-class! [id class]
  (.. js/document (getElementById id) -classList (remove class)))

(defn split-by
  "Returns a vector with two elements: the first is the collection of
  elements for which the predicate returned a truthy value, the second
  for the others."
  [pred coll]
  (let [{survivors true others false} (group-by (comp boolean pred) coll)]
    [survivors others]))

(defn map-vals
  "Returns a new map where all the keys have been updated by f"
  [coll f]
  (into {} (for [[k v] coll] [k (f v)])))

(defn remove-keys
  "Returns a map without the keys that won't pass the predicate"
  [coll pred]
  (into {} (remove (fn [[k _]] (pred k)) coll)))
