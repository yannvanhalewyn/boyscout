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

    (testing "It can mark a position as the source"
      (is (sut/source? (sut/set-source board [1 0]) [1 0])))

    (testing "It can mark a position as the target"
      (is (sut/target? (sut/set-target board [1 0]) [1 0])))))

(deftest walls
  (testing "It can add walls represented as nodes that have no edge to any neighbors"
    (let [board (sut/make-wall (sut/make 3 2) [1 1])]
      (is (empty? (sut/neighbor-coords board [1 1])))
      (is (sut/wall? board [1 1]))))

  (testing "It can remove walls"
    (let [board (sut/make 3 2)
          board2 (-> board (sut/make-wall [1 1]) (sut/destroy-wall [1 1]))]
      (is (= board board2))
      (is (seq (sut/neighbor-coords board [1 1])))
      (is (not (sut/wall? board [1 1])))))

  (testing "Destroying walls won't reconnect the cell to an adjacent wall"
    (let [board (-> (sut/make 3 2)
                    (sut/make-wall [1 1])
                    (sut/make-wall [1 0])
                    (sut/destroy-wall [1 0]))]
      (is (not (contains? (sut/neighbor-coords board [1 0]) [1 1]))))))
