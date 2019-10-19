(ns bs.maze
  (:refer-clojure :exclude [rand-nth divide]))

(defn- rand-nth [coll]
  (when (seq coll) (clojure.core/rand-nth coll)))

(defn- in-board? [[w h] [x y]]
  (and (<= 0 x (dec w)) (<= 0 y (dec h))))

(defn- plus [[x y] [x' y']]
  [(+ x x') (+ y y')])

(declare divide)

(defn- wall-candidate? [board-size wall? before-wall after-wall]
  (and (or (not (in-board? board-size before-wall))
           (wall? before-wall))
       (or (not (in-board? board-size after-wall))
           (wall? after-wall))))

(defn- divide-horizontally [walls board-size [width height] offset]
  (let [wall? (set walls)
        candidates (filter #(and (not (zero? %))
                                 (wall-candidate? board-size wall?
                                                  (plus [-1 %] offset)
                                                  (plus [width %] offset)))
                           (range (dec height)))
        south-of (rand-nth candidates)
        door-x (rand-int width)
        new-walls (when south-of
                    (for [x (range width) :when (not= door-x x)]
                      (plus [x south-of] offset)))]
    (as-> (concat walls new-walls) $
      (divide board-size [width south-of] offset $)
      (divide board-size [width (- height south-of 1)]
              (plus offset [0 (inc south-of)]) $))))

(defn- divide-vertically [walls board-size [width height] offset]
  (let [wall? (set walls)
        candidates (filter #(and (not (zero? %))
                                 (wall-candidate? board-size wall?
                                                  (plus [% -1] offset)
                                                  (plus [% height] offset)))
                           (range (dec width)))
        east-of (rand-nth candidates)
        door-y (rand-int height)
        new-walls (when east-of
                    (for [y (range height) :when (not= door-y y)]
                      (plus [east-of y] offset)))]
    (as-> (concat walls new-walls) $
      (divide board-size [east-of height] offset $)
      (divide board-size [(- width east-of 1) height]
              (plus offset [(inc east-of) 0]) $))))

(defn divide
  "Will return a list of walls (in generation order) that would
  represent a maze being generated via recursive-division on a grid of
  width and height"
  ([width height] (divide [width height] [width height] [0 0] []))
  ([board-size [width height :as size] offset walls]
   (if (and (>= width 2) (>= height 2))
     (cond (< width height)
           (divide-horizontally walls board-size size offset)
           (< height width)
           (divide-vertically walls board-size size offset)
           :else (if (zero? (rand-int 2))
                   (divide-horizontally walls board-size size offset)
                   (divide-vertically walls board-size size offset)))
     walls)))

(defn recursive-division
  "First draws walls (clockwise for animation) around the board and
  then proceeds to recursively divide the inner maze."
  [width height]
  (let [walls (concat (for [x (range width)] [x 0])
                      (for [y (range (dec height))] [(dec width) (inc y)])
                      (for [x (reverse (range (dec width)))] [x (dec height)])
                      (for [y (reverse (range (- height 2)))] [0 (inc y)]))]
    (divide [width height] (plus [width height] [-2 -2]) [1 1] walls)))

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
    (print-ascii! w h (divide w h)))
  )
