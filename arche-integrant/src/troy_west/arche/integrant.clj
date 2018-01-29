(ns troy-west.arche.integrant
  (:require [integrant.core :as integrant]
            [troy-west.arche :as arche]
            [qbits.alia :as alia]))

;;;;;;;;;;;
;; Start

(defmethod integrant/init-key :arche/cluster
  [_ config]
  (alia/cluster config))

(defmethod integrant/init-key :arche/connection
  [_ config]
  (arche/connect (:cluster config)
                 (dissoc config :cluster)))

;;;;;;;;;;;
;;; Stop

(defmethod integrant/halt-key! :arche/cluster
  [_ cluster]
  (alia/shutdown cluster))

(defmethod integrant/halt-key! :arche/connection
  [_ connection]
  (alia/shutdown connection))