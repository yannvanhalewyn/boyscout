(ns bs.algorithm-test
  (:require [bs.algorithm :as sut]
            [bs.board :as board]
            [clojure.test :refer [deftest testing is]]))

(def expected-results
  {::sut/dijkstra
   {:path [[0 1] [0 0] [1 0] [2 0] [3 0] [4 0] [4 1] [4 2] [5 2] [5 3]]
    :visitation-order [[0 1] [0 0] [0 2] [1 0] [0 3] [2 0] [1 3] [0 4] [3 0]
                       [2 1] [2 3] [0 5] [4 0] [2 2] [3 3] [1 5] [5 0] [4 1]
                       [2 5] [5 1] [4 2] [3 5] [5 2] [4 5] [5 3]]}

   ::sut/depth-first
   {:path [[0 1] [0 2] [0 3] [0 4] [0 5] [1 5] [2 5] [3 5] [4 5] [5 5] [5 4] [5 3]]
    :visitation-order [[0 1] [0 2] [0 3] [0 4] [0 5] [1 5] [2 5] [3 5] [4 5] [5 5]
                       [5 4] [4 4]]}

   ::sut/breadth-first
   {:path [[0 1] [0 0] [1 0] [2 0] [3 0] [4 0] [4 1] [4 2] [5 2] [5 3]]
    :visitation-order [[0 1] [0 0] [0 2] [1 0] [0 3] [2 0] [1 3] [0 4]
                       [3 0] [2 1] [2 3] [0 5] [4 0] [2 2] [3 3] [1 5]
                       [5 0] [4 1] [2 5] [5 1] [4 2] [3 5] [5 2] [4 5]]}})

(deftest algorithms-test
  ;; Given this board:
  ;;      0   1   2   3   4   5
  ;;    +---+---+---+---+---+---+
  ;;  0 |   |   |   |   |   |   |
  ;;    +---+---+---+---+---+---+
  ;;  1 | S | x |   | x |   |   |
  ;;    +---+---+---+---+---+---+
  ;;  2 |   | x |   | x |   |   |
  ;;    +---+---+---+---+---+---+
  ;;  3 |   |   |   |   | x | T |
  ;;    +---+---+---+---+---+---+
  ;;  4 |   | x | x | x |   |   |
  ;;    +---+---+---+---+---+---+
  ;;  5 |   |   |   |   |   |   |
  ;;    +---+---+---+---+---+---+
  (let [walls [[1 1] [3 1]
               [1 2] [3 2]
               [4 3]
               [1 4] [2 4] [3 4]]
        source [0 1]
        target [5 3]
        make-walls #(reduce board/make-wall %1 %2)
        board (-> (board/make 6 6)
                  (make-walls walls)
                  (board/set-source source)
                  (board/set-target target))
        connected? (fn [[a b]]
                     (contains? (get-in board [:board/edges a]) b))]
    (doseq [alg (map ::sut/key sut/ALL)
            :let [{::sut/keys [path visitation-order]} (sut/process alg board)
                  not-a-wall? (complement (set walls))]]

      (testing (str alg " won't hit any walls")
        (is (every? not-a-wall? path))
        (is (every? not-a-wall? visitation-order)))

      (testing (str alg " won't visit the same cell twice")
        (is (apply distinct? visitation-order))
        (is (apply distinct? path)))

      (testing (str alg " returns a path that starts at the start")
        (is (= source (first path))))

      (testing (str alg " returns a path that ends at the end")
        (is (= target (last path))))

      (testing (str alg " returns a path that is composed of connected cells")
        (is (every? connected? (partition 2 1 path))))

      (testing (str alg " has the expected shortest path")
        (.log js/console alg :path path)
        (is (= path (get-in expected-results [alg :path]))))

      (testing (str alg " has the expected visitation-order")
        (.log js/console alg :visits visitation-order)
        (is (= visitation-order (get-in expected-results [alg :visitation-order]))))

      (testing (str alg " won't have an answer when target is off the grid")
        (is (nil? (sut/process alg (board/set-target board [10 10])))))

      (testing (str alg " won't have an answer when the target is unreachable")
        (let [board (make-walls board [[3 0] [3 5]])]
          (is (nil? (sut/process alg board))))))))
