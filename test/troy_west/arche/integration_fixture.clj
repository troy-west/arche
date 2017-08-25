(ns troy-west.arche.integration-fixture
  (:require [clojure.test :refer :all]
            [ccm-clj.core :as ccm]
            [qbits.alia :as alia]
            [troy-west.arche :as arche]))

(defonce system (atom {}))

(def udts {:arche/asset {:name "asset"}})

(def statements {:arche/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
                 :arche/select-client "SELECT * FROM client WHERE id = :id"
                 :arche/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
                 :arche/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"})

(defn start-system!
  []
  (ccm/auto-cluster! "arche"
                     "2.2.6"
                     3
                     [#"test-resources/test-keyspace\.cql"]
                     {"sandbox" [#"test-resources/test-tables\.cql"]}
                     19142)
  (let [cluster    (alia/cluster {:contact-points ["127.0.0.1"]
                                  :port           19142})
        connection (arche/connect cluster {:keyspace   "sandbox"
                                           :statements [statements]
                                           :udts       [udts]})]
    (reset! system {:cluster    cluster
                    :connection connection})))

(defn stop-system!
  []
  (alia/shutdown (:connection @system))
  (alia/shutdown (:cluster @system))
  (ccm/stop!)
  (reset! system {}))

(defn connection
  []
  (:connection @system))

(defn wrap-test
  [test-fn]
  (start-system!)
  (test-fn)
  (stop-system!))