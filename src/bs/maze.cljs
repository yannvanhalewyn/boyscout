(ns bs.maze
  (:refer-clojure :exclude [rand-nth]))

(defn- rand-nth [coll]
  (when (seq coll) (clojure.core/rand-nth coll)))

(defn- in-board? [[w h] [x y]]
  (and (<= 0 x (dec w)) (<= 0 y (dec h))))

(declare recursive-division)

(defn- wall-candidate? [board-size wall? before-wall after-wall]
  (and (or (not (in-board? board-size before-wall))
           (wall? before-wall))
       (or (not (in-board? board-size after-wall))
           (wall? after-wall))))

(defn- divide-horizontally [walls board-size [w h] [offset-x offset-y]]
  (let [wall? (set walls)
        candidates (filter #(and (not (zero? %))
                                 (wall-candidate?
                                  board-size wall?
                                  [(dec offset-x) (+ offset-y %)]
                                  [(+ w offset-x) (+ offset-y %)]))
                           (range (dec h)))
        y (rand-nth candidates)
        door-x (rand-int w)
        new-walls (when y
                    (for [x (range w) :when (not= door-x x)]
                      [(+ offset-x x) (+ offset-y y)]))]
    (as-> (concat walls new-walls) $
      (recursive-division board-size [w y] [offset-x offset-y] $)
      (recursive-division board-size [w (- h y 1)] [offset-x (+ offset-y y 1)] $))))

(defn- divide-vertically [walls board-size [w h] [offset-x offset-y]]
  (let [wall? (set walls)
        candidates (filter #(and (not (zero? %))
                                 (wall-candidate?
                                  board-size wall?
                                  [(+ offset-x %) (dec offset-y)]
                                  [(+ offset-x %) (+ h offset-y)]))
                           (range (dec w)))
        x (rand-nth candidates)
        door-y (rand-int h)
        new-walls (when x
                    (for [y (range h) :when (not= door-y y)]
                      [(+ offset-x x) (+ offset-y y)]))]
    (as-> (concat walls new-walls) $
      (recursive-division board-size [x h] [offset-x offset-y] $)
      (recursive-division board-size [(- w x 1) h] [(+ offset-x x 1) offset-y] $))))

(defn recursive-division
  "Will return a list of walls (in generation order) that would
  represent a maze being generated via recursive-division on a grid of
  width and height"
  ([width height] (recursive-division [width height] [width height] [0 0] []))
  ([board-size [width height :as size] offset walls]
   (when (< 1000 (count walls))
     (throw "Infinite loop"))
   (if (and (>= width 2) (>= height 2))
     (cond (< width height)
           (divide-horizontally walls board-size size offset)
           (< height width)
           (divide-vertically walls board-size size offset)
           :else (if (zero? (rand-int 2))
                   (divide-horizontally walls board-size size offset)
                   (divide-vertically walls board-size size offset)))
     walls)))

(comment
  (defn- ->ascii [w h walls]
    (let [wall? (set walls)]
      (for [y (range h)]
        (for [x (range w)]
          (if (wall? [x y]) "x" " ")))))

  (defn- print-ascii! [w h walls]
    (let [sep (str "|" (clojure.string/join "+" (repeat w "---")) "|")]
      (println "")
      (println sep)
      (doseq [row (->ascii w h walls)]
        (println "|" (clojure.string/join " | " row) "|")
        (println sep))
      (println "")))

  (let [[w h] [5 5]]
    (print-ascii! w h (recursive-division w h)))
  )
