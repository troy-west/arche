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
  (let [cluster (alia/cluster {:contact-points ["127.0.0.1"] :port 19142})
        session (arche/connect cluster "sandbox" {:statements statements :udts udts})]
    (reset! system {:cluster cluster :session session})))

(defn stop-system!
  []
  (alia/shutdown (:session @system))
  (alia/shutdown (:cluster @system))
  (ccm/stop!)
  (reset! system {}))

(use-fixtures :each (fn [test-fn]
                      (when-not @system
                        (start-system!)
                        (test-fn))))

(use-fixtures :once (fn [test-fn]
                      (test-fn)
                      (stop-system!)))

(deftest ^:integration session-wrapper-test
  (is (instance? Session (:session @system))))

(deftest ^:integration encoders-test
  (let [encoded (arche/encode (:session @system)
                              ::asset {:code     "AB"
                                       :currency "GBP"
                                       :notional "12"})]
    (is (instance? UDTValue encoded))))

(deftest ^:integration statement-query-test
  (let [session (:session @system)]
    (alia/execute session ::insert-client {:values {:id "id-1" :name "Carol"}})
    (is (= {:id "id-1", :name "Carol"}
           (first (alia/execute session ::select-client {:values {:id "id-1"}}))))))
