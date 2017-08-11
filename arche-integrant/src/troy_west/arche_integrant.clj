(ns troy-west.arche-integrant
  (:require [integrant.core :as ig]
            [troy-west.arche :as arche]
            [troy-west.arche-hugcql :as arche-hugcql]
            [qbits.alia :as alia]))

(defmethod ig/init-key :cassandra/cluster
  [_ config]
  (alia/cluster config))

(defmethod ig/init-key :cassandra/session
  [_ config]
  (arche/initialize-connection config))

(defmethod ig/init-key :arche/statements
  [_ config]
  (arche-hugcql/prepared-statements config))

(defmethod ig/init-key :arche/udts
  [_ config]
  (identity config))

(defmethod ig/halt-key! :cassandra/cluster
  [_ cluster]
  (alia/shutdown cluster))

(defmethod ig/halt-key! :cassandra/session
  [_ session]
  (alia/shutdown session))

(defn session
  [cassandra session-key]
  (second (ig/find-derived-1 cassandra session-key)))

(defn encode
  [session udts-key value]
  (arche/encode-udt session udts-key value))
