(ns troy-west.arche.spec
  (:require [clojure.future :refer [any?]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as spec.test]
            [troy-west.arche :as arche]
            [qbits.alia :as alia])
  (:import (com.datastax.driver.core SessionManager Cluster)))

(s/def ::cluster #(= (type %) Cluster))
(s/def ::connection any?)
(s/def ::session #(= (type %) SessionManager))
(s/def ::statements (s/map-of keyword? string?))
(s/def :udt/name string?)
(s/def :udt/codec any?)
(s/def ::udt (s/keys :req-un [:udt/name] :opt-un [:udt/codec]))
(s/def ::udts (s/map-of keyword? ::udt))
(s/def :connect/statements (s/coll-of ::statements))
(s/def :connect/udts (s/coll-of ::udts))
(s/def ::keyspace string?)
(s/def ::query any?)
(s/def ::alia/execute-opts map?)
(s/def ::execute-args (s/or :first (s/cat :connection ::connection
                                          :query ::query)
                            :second (s/cat :connection ::connection
                                           :query ::query
                                           :opts (s/or :nil nil?
                                                       :opts ::alia/execute-opts))))

(s/fdef troy-west.arche/prepare-statements
        :args (s/cat :session ::session
                     :statements (s/or :nil nil?
                                       :statements ::statements)))

(s/fdef troy-west.arche/prepare-encoders
        :args (s/cat :session ::session
                     :udts (s/or :nil nil?
                                 :udts ::udts)))

(s/fdef troy-west.arche/statement
        :args (s/cat :connection ::connection
                     :key any?))

(s/fdef troy-west.arche/udt-encoder
        :args (s/cat :connection ::connection
                     :key any?))

(s/fdef troy-west.arche/encode-udt
        :args (s/cat :connection ::connection
                     :key any?
                     :value any?))

(s/fdef troy-west.arche/connect
        :args (s/cat :cluster ::cluster
                     :opts (s/? (s/keys :opt-un [::keyspace :connect/statements :connect/udts]))))

(s/fdef troy-west.arche/disconnect
        :args (s/cat :connection ::connection))

(s/fdef troy-west.arche/execute
        :args ::execute-args)

(s/fdef troy-west.arche/execute-async
        :args ::execute-args)

(defn instrument!
  []
  (spec.test/instrument '[troy-west.arche/prepare-statements
                          troy-west.arche/prepare-encoders
                          troy-west.arche/statement
                          troy-west.arche/udt-encoder
                          troy-west.arche/encode-udt
                          troy-west.arche/connect
                          troy-west.arche/disconnect
                          troy-west.arche/execute
                          troy-west.arche/execute-async]))
