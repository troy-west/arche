(ns troy-west.arche.component
  (:require [troy-west.arche :as arche]
            [troy-west.arche-hugcql :as arche-hugcql]
            [com.stuartsierra.component :as component]
            [qbits.alia :as alia])
  (:import (com.datastax.driver.core Cluster)))

(defrecord Statements [config]
  component/Lifecycle

  (start [this]
    (arche-hugcql/prepared-statements config)))

(extend-type Cluster
  component/Lifecycle
  (start [this]
    (.init this))
  (stop [this]
    (.close this)
    this))

(defrecord Connection [keyspace cluster statements udts connection]
  component/Lifecycle

  (start [this]
    (assoc this ::connection (arche/connect cluster {:keyspace   keyspace
                                                     :statements statements
                                                     :udts       udts})))

  (stop [this]
    (when connection (alia/shutdown {:session connection}))))

;;;;;;;;;;;;;
;;; Public

(defn create-statements
  [config]
  (->Statements config))

(defn create-cluster
  [config]
  (alia/cluster config))

(defn create-connection
  [{:keys [keyspace cluster statements udts]}]
  (component/using
    (map->Connection {:keyspace keyspace})
    {:cluster    cluster
     :statements statements
     :udts       udts}))

(defn connection
  [cassandra session-key]
  (-> cassandra session-key ::connection))
