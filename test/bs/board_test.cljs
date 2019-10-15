(ns bs.board-test
  (:require [bs.board :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest board
  (let [board (sut/make 3 2)]
    (testing "Make returns a board as a graph of x * y where all cells are connected"
      (is (= [[0 0] [0 1]
              [1 0] [1 1]
              [2 0] [2 1]]
             (sut/all-coordinates board)))
      (is (= #{[0 1] [1 0] [2 1]} (sut/neighbor-coords board [1 1]))))

    (testing "It can mark a position as visited"
      (is (sut/visited? (sut/mark-visited board [1 0]) [1 0])))

    (testing "It can mark a position as a path"
      (is (sut/path? (sut/mark-path board [1 0]) [1 0])))

    (testing "It can replace the path"
      (let [board-with-path (sut/mark-path board [1 1])
            board (sut/set-path board-with-path [[0 0] [0 1]])]
        (is (sut/path? board [0 0]))
        (is (sut/path? board [0 1]))
        (is (not (sut/path? board [1 1])))))

    (testing "It can replace the visited cells"
      (let [board-with-visited (sut/mark-visited board [1 1])
            board (sut/set-visited board-with-visited [[0 0] [0 1]])]
        (is (sut/visited? board [0 0]))
        (is (sut/visited? board [0 1]))
        (is (not (sut/visited? board [1 1])))))

    (testing "It can mark a position as the source"
      (is (sut/source? (sut/set-source board [1 0]) [1 0])))

    (testing "It can mark a position as the target"
      (is (sut/target? (sut/set-target board [1 0]) [1 0]))))

  (testing "It can add walls represented as nodes that have no edge to any neighbors"
    (let [board (sut/make-wall (sut/make 3 2) [1 1])]
      (is (empty? (sut/neighbor-coords board [1 1])))
      (is (sut/wall? board [1 1])))))
