(ns bs.board-test
  (:require [bs.board :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest board
  (testing "It returns a board of x * y where all cells are connected"
    (let [board (sut/make 3 2)]
      (is (= [[0 0] [0 1]
              [1 0] [1 1]
              [2 0] [2 1]]
             (sut/all-coordinates board)))
      (is (= #{[0 1] [1 0] [2 1]} (sut/neighbor-coords board [1 1])))
      (is (sut/visited? (sut/mark-visited board [1 0]) [1 0]))
      (is (sut/path? (sut/mark-path board [1 0]) [1 0]))
      (is (sut/source? (sut/set-source board [1 0]) [1 0]))
      (is (sut/target? (sut/set-target board [1 0]) [1 0]))))

  (testing "It can add walls, which are represented as nodes that have no edge to any neighbors"
    (let [board (sut/make-wall (sut/make 3 2) [1 1])]
      (is (empty? (sut/neighbor-coords board [1 1])))
      (is (sut/wall? board [1 1])))))
