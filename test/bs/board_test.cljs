(ns bs.board-test
  (:require [bs.board :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest in-board?-helper-test-for-workshop
  (testing "a point in the board"
    (is (true? (#'sut/in-board? 10 10 [5 5]))))

  (testing "a point with negative dimensions"
    (is (false? (#'sut/in-board? 10 10 [-1 0])))
    (is (false? (#'sut/in-board? 10 10 [0 -1]))))

  (testing "a point outside of the width and height"
    (is (false? (#'sut/in-board? 10 10 [20 20])))
    (is (false? (#'sut/in-board? 10 10 [10 5])))
    (is (false? (#'sut/in-board? 10 10 [5 10])))))

(deftest adjacent-coords-helper-test-for-workshop
  (testing "returns the four adjacent points"
    (is (= #{[1 0] [2 1] [1 2] [0 1]}
           (set (#'sut/adjacent-coords 5 5 [1 1])))))

  (testing "won't return points outside the board"
    (is (= #{[3 4] [4 3]} (set (#'sut/adjacent-coords 5 5 [4 4]))))
    (is (= #{[0 1] [1 0]} (set (#'sut/adjacent-coords 5 5 [0 0]))))))

(deftest making-a-board
  (let [board (sut/make 3 2)]
    (testing "It makes a board of given width and height"
      (is (= #{[0 0] [1 0] [2 0]
               [0 1] [1 1] [2 1]}
             (set (sut/all-coordinates board)))))

    (testing "It makes a board where all neighboring cells are connected"
      (is (= #{[1 0] [2 1] [0 1]} (sut/neighbor-coords board [1 1]))))

    (testing "It has a width and a height"
      (is (= 3 (:board/width board))))

    (testing "It has a height"
      (is (= 2 (:board/height board))))))

(deftest setting-source-and-target
  (let [board (sut/make 3 2)]
    (testing "It can mark a position as the source"
      (is (sut/source? (sut/set-source board [1 0]) [1 0])))

    (testing "It can mark a position as the target"
      (is (sut/target? (sut/set-target board [1 0]) [1 0])))))

#_(deftest walls
    (let [wall [1 1]
          board (sut/make-wall (sut/make 3 2) wall)]
      (testing "It can make a wall"
        (is (sut/wall? board wall)))

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
