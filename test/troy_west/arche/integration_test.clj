(ns troy-west.arche.integration-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche :as arche]
            [troy-west.arche.integration-fixture :as fixture]
            [ccm-clj.core :as ccm])
  (:import [com.datastax.driver.core UDTValue Session]))

(use-fixtures :once fixture/wrap-test)

(deftest encoders-test

  (let [encoded (arche/encode-udt (fixture/connection)
                                  :arche/asset
                                  {:code     "AB"
                                   :currency "GBP"
                                   :notional "12"})]

    (is (instance? UDTValue encoded))))

(deftest insert-select-test

  (let [connection (fixture/connection)]

    (is (= {:id "id-1", :name "Carol"}
           (do (arche/execute connection
                              :arche/insert-client
                              {:values {:id "id-1" :name "Carol"}})
               (first (arche/execute connection
                                     :arche/select-client
                                     {:values {:id "id-1"}})))))))