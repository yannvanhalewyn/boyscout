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
      (is (= [[1 0] [2 1] [0 1]] (sut/neighbor-coords board [1 1]))))

    (testing "It can mark a position as the source"
      (is (sut/source? (sut/set-source board [1 0]) [1 0])))

    (testing "It can mark a position as the target"
      (is (sut/target? (sut/set-target board [1 0]) [1 0])))))

(deftest walls
  (let [wall [1 1]
        board (sut/make-wall (sut/make 3 2) wall)]
    (testing "It can make a wall"
      (is (sut/wall? board [1 1])))

    (testing "A wall has no neighbors"
      (is (empty? (sut/neighbor-coords board wall))))

    (testing "No cell on the board is connected to a wall"
      (is (not-any? #(contains? (set (sut/neighbor-coords board %)) wall)
                    (sut/all-coordinates board)))))

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

(deftest weights
  (let [board (sut/set-weight (sut/make 3 2) [1 1] [1 2] 10)]
    (testing "It sets a weight on the edge between from and to"
      (is (= 10 (get (sut/neighbors board [1 1]) [1 2]))))))

(deftest forests
  (let [board (-> (sut/make 3 2)
                  (sut/make-forest [1 1])
                  (sut/make-wall [0 0]))]
    (testing "It can make a forest"
      (is (not (sut/forest? board [0 0])))
      (is (sut/forest? board [1 1])))

    (testing "It sets the weight from every neighbor to the forest cell"
      (is (every? #{sut/FOREST_WEIGHT}
                  (map #(get (sut/neighbors board %) [1 1])
                       (sut/neighbor-coords board [1 1])))))

    (testing "It can remove forests"
      (is (not (sut/forest? (sut/unilever board [1 1]) [1 1]))))

    (testing "It can remove forests without killing it's neighbor forests"
      (is (-> board
              (sut/make-forest [1 0])
              (sut/unilever [1 1])
              (sut/forest? [1 0]))))))
