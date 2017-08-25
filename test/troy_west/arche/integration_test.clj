(ns troy-west.arche.integration-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche :as arche]
            [troy-west.arche.integration-fixture :as fixture]
            [ccm-clj.core :as ccm]
            [qbits.alia :as alia])
  (:import [com.datastax.driver.core UDTValue Session]))

(use-fixtures :once (fn [test-fn]
                      (fixture/start-system!)
                      (test-fn)
                      (fixture/stop-system!)))

(deftest connection-wrapper-test
  (is (instance? Session (:connection @fixture/system))))

(deftest encoders-test
  (let [encoded (arche/encode-udt (:connection @fixture/system)
                                  ::asset {:code     "AB"
                                           :currency "GBP"
                                           :notional "12"})]
    (is (instance? UDTValue encoded))))

(deftest statement-query-test
  (let [session (:connection @fixture/system)]
    (alia/execute session ::insert-client {:values {:id "id-1" :name "Carol"}})
    (is (= {:id "id-1", :name "Carol"}
           (first (alia/execute session ::select-client {:values {:id "id-1"}}))))))