(ns troy-west.arche-test
  (:require [clojure.test :refer [deftest is]]
            [qbits.alia :as alia])
  (:import com.datastax.driver.core.SimpleStatement
           troy_west.arche.IBindable))

(deftest query->statement-test
  (let [statement (alia/query->statement ::test {} nil)]
    (is (instance? SimpleStatement statement))
    (is (instance? troy_west.arche.IBindable statement))))
