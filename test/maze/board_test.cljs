(ns maze.board-test
  (:require [maze.board :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest board
  (testing "It returns a board of x * y where all cells are connected"
    (let [board (sut/make 3 2)]
      (is (= [[0 0] [0 1]
              [1 0] [1 1]
              [2 0] [2 1]]
             (sut/all-coordinates board)))
      (is (= #{[0 1] [1 0] [2 1]} (sut/neighbor-coords board [1 1])))
      (is (true? (get-in (sut/set-start board [0 1]) [[0 1] :cell/start?])))
      (is (true? (get-in (sut/set-end board [1 1]) [[1 1] :cell/end?])))
      (is (true? (get-in (sut/mark-visited board [0 0]) [[0 0] :cell/visited?]))))))
