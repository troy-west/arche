(ns troy-west.arche.spec
  (:require [clojure.future :refer [any?]]
            [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as spec.test]
            [troy-west.arche :as arche])
  (:import (com.datastax.driver.core SessionManager Cluster PreparedStatement)))

;; Configuration
(spec/def :arche.config.udt/name string?)
(spec/def :arche.config.udt/codec any?)
(spec/def :arche.config/udt (spec/keys :req-un [:arche.config.udt/name] :opt-un [:arche.config.udt/codec]))
(spec/def :arche.config/udts (spec/or :udts (spec/map-of keyword? :arche.config/udt)
                                      :udts (spec/coll-of (spec/map-of keyword? :arche.config/udt))))

(spec/def :arche.config/statement (spec/or :statement :arche/cql
                                           :statement (spec/keys :req-un [:arche/cql]
                                                                 :opt-un [:alia.execute/opts])))
(spec/def :arche.config/statements (spec/or :statements (spec/map-of keyword? :arche.config/statement)
                                            :statements (spec/coll-of (spec/map-of keyword? :arche.config/statement))))

(spec/def :cassandra/keyspace string?)

;; State

(spec/def :cassandra/cluster #(= (type %) Cluster))
(spec/def :cassandra/session #(= (type %) SessionManager))

(spec/def :alia.execute/opts map?)

(spec/def :arche/cql string?)
(spec/def :arche/prepared #(instance? PreparedStatement %))

(spec/def :arche/statement-key keyword?)
(spec/def :arche/statement (spec/keys :req-un [:arche/cql :arche/prepared]
                                      :opt-un [:alia.execute/opts]))

(spec/def :arche/statements (spec/map-of :arche/statement-key :arche/statement))

(spec/def :arche/udt-encoder ifn?)
(spec/def :arche/udt-encoders (spec/coll-of :arche/udt-encoder))

(spec/def :arche/connection (spec/keys :req-un [:cassandra/session :arche/statements :arche/udt-encoders]))

;; Execution
(spec/def ::execute-args (spec/or :first (spec/cat :connection :arche/connection
                                                   :key :arche/statement-key)
                                  :second (spec/cat :connection :arche/connection
                                                    :key :arche/statement-key
                                                    :opts (spec/or :nil nil?
                                                                   :opts :alia.execute/opts))))

(spec/fdef troy-west.arche/prepare-statements
           :args (spec/cat :session :cassandra/session
                           :statements (spec/or :nil nil?
                                                :statements :arche.config/statements)))

(spec/fdef troy-west.arche/prepare-encoders
           :args (spec/cat :session :cassandra/session
                           :udts (spec/or :nil nil? :udts :arche.config/udts)))

(spec/fdef troy-west.arche/encode-udt
           :args (spec/cat :connection :arche/connection
                           :key any?
                           :value any?))

(spec/fdef troy-west.arche/connect
           :args (spec/cat :cluster :cassandra/cluster
                           :opts (spec/? (spec/keys :opt-un [:cassandra/keyspace
                                                             :arche.config/statements
                                                             :arche.config/udts]))))

(spec/fdef troy-west.arche/disconnect
           :args (spec/cat :connection :cassandra/connection))

(spec/fdef troy-west.arche/execute :args ::execute-args)

(spec/fdef troy-west.arche/execute-async :args ::execute-args)

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
