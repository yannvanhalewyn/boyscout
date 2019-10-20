(ns bs.utils)

(defn no-op [& _])

(defn add-class! [id class]
  (.. js/document (getElementById id) -classList (add class)))

(defn remove-class! [id class]
  (.. js/document (getElementById id) -classList (remove class)))

(defn resource-path
  "Bit hacky for now, Github pages and my dev setup have a bit
  different file structure, so just check if it's a dev or prod build
  and return a prefix for any resource"
  [filename]
  (if js/goog.DEBUG
    (str "/" filename)
    (str "./build/" filename)))

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
  (into {} (for [[k v] coll] [k (f v k)])))

(defn remove-keys
  "Returns a map without the keys that won't pass the predicate"
  [coll pred]
  (into {} (remove (fn [[k _]] (pred k)) coll)))

(def abs (.-abs js/Math))

(defn manhattan-distance
  "The distance if you we're to walk horizontally or vertically on a
  grid from [x y] to [x' y']"
  [[x y] [x' y']]
  (+ (abs (- x' x)) (abs (- y' y))))
