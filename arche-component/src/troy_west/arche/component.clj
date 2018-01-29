(ns troy-west.arche.component
  (:require [troy-west.arche :as arche]
            [qbits.alia :as alia]
            [com.stuartsierra.component :as cp]))

(defrecord ClusterComponent [config cluster]
  cp/Lifecycle

  (start [this]
    (assoc this :cluster (alia/cluster config)))

  (stop [_]
    (alia/shutdown cluster)))

(defrecord ConnectionComponent [config session statements udt-encoders cluster]
  cp/Lifecycle
  (start [this]
    (let [{:keys [session statements udt-encoders]} (arche/connect (:cluster cluster) config)]
      (assoc this
             :session session
             :statements statements
             :udt-encoders udt-encoders)))

  (stop [this]
    (arche/disconnect this)))

;;;;;;;;;;;;;
;;; Public

(defn cluster
  [config]
  (map->ClusterComponent {:config config}))

(defn connection
  [config]
  (let [cluster (:cluster config)]
    (cond-> (map->ConnectionComponent {:config config})
      cluster (cp/using {:cluster cluster}))))

(def data-readers
  {'arche/cluster    cluster
   'arche/connection connection})