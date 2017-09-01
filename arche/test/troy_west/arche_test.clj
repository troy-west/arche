(ns troy-west.arche-test
  (:require [clojure.test :refer [deftest is]]
            [troy-west.arche :as arche]
            [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]))

(deftest prepare-statements

  (is (= {} (arche/prepare-statements nil nil)))

  (is (= {:query-1 "prepared: a-query"
          :query-2 "prepared: b-query"}
         (with-redefs [alia/prepare (fn [_ query] (format "prepared: %s" query))]
           (arche/prepare-statements
             nil
             {:query-1 "a-query"
              :query-2 "b-query"})))))

(deftest prepare-encoders

  (is (= {} (arche/prepare-encoders nil nil)))

  (is (= {:udt-1 {:prepared true}
          :udt-2 {:prepared true}}
         (with-redefs [alia.udt/encoder (fn [_ name codec] {:prepared true})]
           (arche/prepare-encoders
             nil
             {:udt-1 {}
              :udt-2 {}})))))

(deftest statement

  (is (= "a cql string"
         (arche/statement "a cql string" {})))

  (is (= "a prepared statement"
         (arche/statement :key {:statements {:key "a prepared statement"}}))))