(ns drplate.core-test
  (:require [clojure.test :refer :all]
            [drplate.core :refer :all]))

(deftest global-test
  (testing "basic behavior"
    (is (= [] (-main "resources/base.drplate" "resources/result")))))
