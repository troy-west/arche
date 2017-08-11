(ns troy-west.arche-component
  (:require [com.stuartsierra.component :as component]
            [troy-west.arche :as arche]
            [troy-west.arche-hugcql :as arche-hugcql]
            [qbits.alia :as alia]))

(defrecord StatementsComponent [config]
  component/Lifecycle

  (start [this]
    (println ";; Starting Cassandra StatementsComponent")
    (arche-hugcql/prepared-statements config)))

(defn create-statements
  [config]
  (->StatementsComponent config))

(defrecord ClusterComponent [config cluster]
  component/Lifecycle

  (start [this]
    (println ";; Starting Cassandra ClusterComponent")
    (let [cluster (alia/cluster config)]
      (assoc this :cluster cluster)))

  (stop [this]
    (println ";; Stopping Cassandra ClusterComponent")
    (when cluster (alia/shutdown cluster))
    (assoc this :cluster nil)))

(defn create-cluster
  [config]
  (map->ClusterComponent {:config config}))

(defrecord SessionComponent [keyspace cluster statements udts session]
  component/Lifecycle

  (start [this]
    (println ";; Starting Cassandra SessionComponent")
    (let [session (arche/initialize-connection (update this :cluster :cluster))]
      (assoc this :session session)))

  (stop [this]
    (println ";; Stopping Cassandra SessionComponent")
    (when session (alia/shutdown session))
    (assoc this :session nil)))

(defn create-session
  [{:keys [keyspace cluster statements udts]}]
  (component/using
   (map->SessionComponent {:keyspace keyspace})
   {:cluster    cluster
    :statements statements
    :udts       udts}))

(defn session
  [cassandra session-key]
  (-> cassandra session-key :session))

(defn encode
  [session udts-key value]
  (arche/encode-udt session udts-key value))
