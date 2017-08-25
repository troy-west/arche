(ns troy-west.arche.integration-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.core.async :as async]
            [troy-west.arche :as arche]
            [troy-west.arche.async :as arche.async]
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

    (let [id-root     (str mode "-id-")
          client-name (str mode "-name-")]

      ;; vanilla
      (let [client-id (str id-root "vanilla")]
        (is (= {:id   client-id
                :name client-name}
               (do (arche/execute connection
                                  :arche/insert-client
                                  {:values {:id   client-id
                                            :name client-name}})
                   (first (arche/execute connection
                                         :arche/select-client
                                         {:values {:id client-id}}))))))

      ;; chan-buffered (t-w's preferred approach)
      (let [client-id (str id-root "async")]
        (is (= {:id   client-id
                :name client-name}
               (do (async/<!! (arche.async/execute-chan-buffered
                                connection
                                :arche/insert-client
                                {:values {:id   client-id
                                          :name client-name}}))
                   (async/<!! (arche.async/execute-chan-buffered
                                connection
                                :arche/select-client
                                {:values {:id client-id}}))))))

      ;; write then read a udt
      (let [trade-id (str id-root "udt")]
        (is (= {:id           trade-id
                :asset-basket {"pork-bellies" {:code     "PB"
                                               :currency "GBP"
                                               :notional "12"}}}
               (let [asset-udt (arche/encode-udt connection
                                                 :arche/asset
                                                 {:code     "PB"
                                                  :currency "GBP"
                                                  :notional "12"})]
                 (arche/execute connection
                                :arche/insert-trade
                                {:values {:id           trade-id
                                          :asset-basket {"pork-bellies" asset-udt}}})
                 (first (arche/execute connection
                                       :arche/select-trade
                                       {:values {:id trade-id}})))))))))

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