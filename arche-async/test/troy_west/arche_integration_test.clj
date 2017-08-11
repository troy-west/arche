(ns troy-west.arche-integration-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche :as arche]
            [ccm-clj.core :as ccm]
            [qbits.alia :as alia])
  (:import [com.datastax.driver.core UDTValue Session]))

(def udts {::asset {:name "asset"}})

(def statements {::insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
                 ::select-client "SELECT * FROM client WHERE id = :id"
                 ::insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
                 ::select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"})

(defonce system (atom {}))

(defn start-system!
  []
  (ccm/auto-cluster! "arche"
                     "2.2.6"
                     3
                     [#"test-resources/test-keyspace\.cql"]
                     {"sandbox" [#"test-resources/test-tables\.cql"]}
                     19142)
  (let [cluster    (alia/cluster {:contact-points ["127.0.0.1"] :port 19142})
        connection (arche/connect cluster {:keyspace "sandbox" :statements statements :udts udts})]
    (reset! system {:cluster cluster :connection connection})))

(defn stop-system!
  []
  (alia/shutdown (:connection @system))
  (alia/shutdown (:cluster @system))
  (ccm/stop!)
  (reset! system {}))

(use-fixtures :once (fn [test-fn]
                      (start-system!)
                      (test-fn)
                      (stop-system!)))

(deftest connection-wrapper-test
  (is (instance? Session (:connection @system))))

(deftest encoders-test
  (let [encoded (arche/encode-udt (:connection @system)
                                  ::asset {:code     "AB"
                                           :currency "GBP"
                                           :notional "12"})]
    (is (instance? UDTValue encoded))))

(deftest statement-query-test
  (let [session (:connection @system)]
    (alia/execute session ::insert-client {:values {:id "id-1" :name "Carol"}})
    (is (= {:id "id-1", :name "Carol"}
           (first (alia/execute session ::select-client {:values {:id "id-1"}}))))))
