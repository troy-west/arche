(ns troy-west.arche.integrant
  (:require [integrant.core :as ig]
            [troy-west.arche :as arche]
            [qbits.alia :as alia]))

;;;;;;;;;;;
;; Start

(defmethod ig/init-key :cassandra/cluster
  [_ config]
  (alia/cluster config))

(defmethod ig/init-key :cassandra/connection
  [_ config]
  (arche/connect (:cluster config)
                 (dissoc config :cluster)))

;;;;;;;;;;;
;;; Stop

(defmethod ig/halt-key! :cassandra/cluster
  [_ cluster]
  (alia/shutdown cluster))

(defmethod ig/halt-key! :cassandra/connection
  [_ connection]
  (alia/shutdown connection))