(ns com.troy-west.arche-integrant
  (:require [integrant.core :as ig]
            [com.troy-west.arche :as arche]
            [com.troy-west.arche-hugsql :as arche-hugsql]
            [qbits.alia :as alia]))

(defmethod ig/init-key :cassandra/cluster
  [_ config]
  (alia/cluster config))

(defmethod ig/init-key :cassandra/session
  [_ config]
  (arche/init-session config))

(defmethod ig/init-key :arche/statements
  [_ config]
  (arche-hugsql/prepared-statements config))

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
  (arche/encode session udts-key value))
