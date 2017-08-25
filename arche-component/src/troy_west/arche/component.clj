(ns troy-west.arche.component
  (:require [troy-west.arche :as arche]
            [com.stuartsierra.component :as component]
            [qbits.alia :as alia])
  (:import (com.datastax.driver.core Cluster)))

(defrecord ClusterComponent [config cluster]
  component/Lifecycle

  (start [this]
    (assoc this :cluster (alia/cluster config)))

  (stop [this]
    (alia/shutdown cluster)))

(defrecord ConnectionComponent [config session statements udt-encoders cluster]
  component/Lifecycle
  (start [this]
    (let [{:keys [session statements udt-encoders]} (arche/connect cluster config)]
      (assoc this :session session
                  :statements statements
                  :udt-encoders udt-encoders)))

  (stop [this]
    (alia/shutdown session)))

;;;;;;;;;;;;;
;;; Public

(defn cluster
  [config]
  (map->ClusterComponent {:config config}))

(defn connection
  [config]
  (let [cluster (:cluster config)]
    (cond-> (map->ConnectionComponent {:config config})
            cluster (component/using {:cluster cluster}))))