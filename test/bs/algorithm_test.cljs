(ns bs.algorithm-test
  (:require [bs.algorithm :as sut]
            [bs.board :as board]
            [clojure.test :refer [deftest testing is]]))

(deftest properties-test
  (doseq [alg sut/ALL]
    (testing (str alg " won't have an answer when target is off the grid")
      (is (nil? (sut/process alg (board/make 4 4) [1 0] [10 10]))))

    (testing (str alg " won't have an answer when the target is unreachable")
      (let [board (-> (board/make 4 4)
                      (board/make-wall [2 3])
                      (board/make-wall [3 2]))]
        (is (nil? (sut/process alg board [1 0] [3 3])))))))

(deftest dijkstra-test
  (testing "Example from corner to corner"
    (is (= {::sut/visitation-order [[0 0] [0 1] [1 0] [0 2] [1 1]
                                    [2 0] [1 2] [2 1] [2 2]]
            ::sut/shortest-path [[0 0] [1 0] [2 0] [2 1] [2 2]]}
           (sut/process ::sut/dijkstra (board/make 3 3) [0 0] [2 2]))))

  (testing "Example between two other points"
    (is (= {::sut/visitation-order [[1 0] [0 0] [1 1] [2 0] [0 1] [1 2] [2 1]
                                    [3 0] [0 2] [1 3] [2 2] [3 1] [0 3] [2 3]]
            ::sut/shortest-path [[1 0] [2 0] [2 1] [2 2] [2 3]]}
           (sut/process ::sut/dijkstra (board/make 4 4) [1 0] [2 3])))))

(deftest depth-first-test
  (testing "Example from corner to corner"
    (is (= {::sut/shortest-path [[0 0] [1 0] [2 0] [2 1] [2 2]]
            ::sut/visitation-order [[0 0] [1 0] [2 0] [2 1] [2 2]]}
           (sut/process ::sut/depth-first (board/make 3 3) [0 0] [2 2]))))

  (testing "Example between two other points"
    (is (= {::sut/shortest-path [[1 0] [2 0] [3 0] [3 1] [3 2] [3 3] [2 3]]
            ::sut/visitation-order [[1 0] [2 0] [3 0] [3 1] [3 2] [3 3] [2 3]]}
           (sut/process ::sut/depth-first (board/make 4 4) [1 0] [2 3])))))
