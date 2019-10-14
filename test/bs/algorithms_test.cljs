(ns bs.algorithms-test
  (:require [bs.algorithms :as sut]
            [bs.board :as board]
            [clojure.test :refer [deftest testing is]]))

(deftest dijkstra-test
  (is (= {::sut/visitation-order [[0 1] [1 1] [0 0] [2 1] [1 0] [2 0]]
          ::sut/shortest-path [[0 1] [0 0] [1 0] [2 0]]}
         (sut/dijkstra (board/make 3 2) [0 1] [2 0])))

  (is (= {::sut/visitation-order [[1 0] [2 0] [1 1] [0 0] [2 1] [3 0] [1 2]
                                  [0 1] [0 2] [3 1] [1 3] [2 2] [0 3] [2 3]]
          ::sut/shortest-path [[1 0] [1 1] [1 2] [2 2] [2 3]]}
         (sut/dijkstra (board/make 4 4) [1 0] [2 3])))

  (testing "It won't have an answer when target is off the grid"
    (is (nil? (sut/dijkstra (board/make 4 4) [1 0] [10 10]))))

  (testing "It won't have an answer when the target is unreachable"
    (let [board (-> (board/make 4 4)
                    (board/make-wall [2 3])
                    (board/make-wall [3 2]))]
      (is (nil? (sut/dijkstra board [1 0] [3 3]))))))
