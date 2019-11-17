(ns bs.board-test
  (:require [bs.board :as sut]
            [clojure.test :refer [deftest testing is]]))

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
