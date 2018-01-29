(ns troy-west.arche.integrant
  (:require [integrant.core :as ig]
            [troy-west.arche :as arche]
            [qbits.alia :as alia]))

;;;;;;;;;;;
;; Start

(defmethod ig/init-key :arche/cluster
  [_ config]
  (alia/cluster config))

(defmethod ig/init-key :arche/connection
  [_ config]
  (arche/connect (:cluster config)
                 (dissoc config :cluster)))

;;;;;;;;;;;
;;; Stop

(defmethod ig/halt-key! :arche/cluster
  [_ cluster]
  (alia/shutdown cluster))

(defmethod ig/halt-key! :arche/connection
  [_ connection]
  (alia/shutdown connection))