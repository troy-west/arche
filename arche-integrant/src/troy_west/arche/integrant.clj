(ns troy-west.arche.integrant
  (:require [integrant.core :as ig]
            [troy-west.arche :as arche]
            [troy-west.arche-hugcql :as arche-hugcql]
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

(defmethod ig/init-key :arche/statements
  [_ config]
  (arche-hugcql/prepared-statements config))

(defmethod ig/init-key :arche/udts
  [_ config]
  (identity config))

;;;;;;;;;;;
;;; Stop

(defmethod ig/halt-key! :cassandra/cluster
  [_ cluster]
  (alia/shutdown cluster))

(defmethod ig/halt-key! :cassandra/connection
  [_ connection]
  (alia/shutdown connection))

;;;;;;;;;;;;;;
;;; Utility

(defn connection
  [cassandra session-key]
  (second (ig/find-derived-1 cassandra session-key)))