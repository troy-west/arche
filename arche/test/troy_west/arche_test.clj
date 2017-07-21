(ns troy-west.arche-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche :as arche]
            [qbits.alia :as alia])
  (:import [com.datastax.driver.core SimpleStatement]))

(deftest query->statement-test
  (let [statement (alia/query->statement ::test {} nil)]
    (is (instance? SimpleStatement statement))
    (is (instance? troy_west.arche.IBindable statement))))

(deftest failing-test
  (is (= 0 1)))
