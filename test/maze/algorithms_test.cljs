(ns maze.algorithms-test
  (:require [maze.algorithms :as sut]
            [maze.board :as board]
            [clojure.test :refer [deftest is]]))

(deftest dijkstra-test
  (is (= {::sut/visitation-order [[0 1] [1 1] [0 0] [2 1] [1 0] [2 0]]
          ::sut/fastest-path [[0 1] [0 0] [1 0] [2 0]]}
         (sut/dijkstra (board/make 3 2) [0 1] [2 0])))

  (is (= {::sut/visitation-order [[1 0] [2 0] [1 1] [0 0] [2 1] [3 0] [1 2]
                                  [0 1] [0 2] [3 1] [1 3] [2 2] [0 3] [2 3]]
          ::sut/fastest-path [[1 0] [1 1] [1 2] [2 2] [2 3]]}
         (sut/dijkstra (board/make 4 4) [1 0] [2 3]))))
