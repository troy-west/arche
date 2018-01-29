(ns troy-west.arche.integrant
  (:require [troy-west.arche :as arche]
            [integrant.core :as ig]
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
  (arche/disconnect connection))

(def data-readers
  {'ig/ref ig/ref})