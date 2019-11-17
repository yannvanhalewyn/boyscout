(ns bs.utils)

(defn no-op [& _])

(defn add-class! [id class]
  (.. js/document (getElementById id) -classList (add class)))

(defn remove-class! [id class]
  (.. js/document (getElementById id) -classList (remove class)))
