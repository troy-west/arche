(ns troy-west.arche-test
  (:require [clojure.test :refer [deftest is]]
            [troy-west.arche :as arche]
            [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]))

(deftest prepare-statements

  (is (= {} (arche/prepare-statements nil nil)))

  (is (= {:query-1 {:cql      "a-query"
                    :prepared "prepared: a-query"}
          :query-2 {:cql      "b-query"
                    :prepared "prepared: b-query"}
          :query-3 {:cql      "c-query"
                    :prepared "prepared: c-query"
                    :opts     {:fetch-size 5000}}}
         (with-redefs [alia/prepare (fn [_ cql] (format "prepared: %s" cql))]
           (arche/prepare-statements
             nil
             {:query-1 "a-query"
              :query-2 {:cql "b-query"}
              :query-3 {:cql  "c-query"
                        :opts {:fetch-size 5000}}})))))

(deftest prepare-encoders

  (is (= {} (arche/prepare-encoders nil nil)))

  (is (= {:udt-1 {:prepared true}
          :udt-2 {:prepared true}}
         (with-redefs [alia.udt/encoder (fn [_ name codec] {:prepared true})]
           (arche/prepare-encoders
             nil
             {:udt-1 {}
              :udt-2 {}})))))