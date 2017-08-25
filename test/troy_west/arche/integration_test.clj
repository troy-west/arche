(ns troy-west.arche.integration-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [troy-west.arche :as arche]
            [troy-west.arche.integration-fixture :as fixture]
            [ccm-clj.core :as ccm])
  (:import [com.datastax.driver.core UDTValue Session]))

(use-fixtures :once fixture/wrap-test)

(defn test-udt-encoding
  [mode connection]
  (testing (str mode ": test UDT encoding")
    (let [encoded (arche/encode-udt connection
                                    :arche/asset
                                    {:code     "AB"
                                     :currency "GBP"
                                     :notional "12"})]

      (is (instance? UDTValue encoded)))))

(defn test-write-then-read
  [mode connection]
  (testing (str mode ": test write then read")
    (is (= {:id "id-1", :name "Carol"}
           (do (arche/execute connection
                              :arche/insert-client
                              {:values {:id "id-1" :name "Carol"}})
               (first (arche/execute connection
                                     :arche/select-client
                                     {:values {:id "id-1"}})))))))

(deftest hand-rolled

  (let [mode       "hand-rolled"
        connection (fixture/connection mode)]

    (test-udt-encoding mode connection)
    (test-write-then-read mode connection)))

(deftest component

  (let [mode       "component"
        connection (fixture/connection mode)]

    (test-udt-encoding mode connection)
    (test-write-then-read mode connection)))

(deftest integrant

  (let [mode       "integrant"
        connection (fixture/connection mode)]

    (test-udt-encoding mode connection)
    (test-write-then-read mode connection)))