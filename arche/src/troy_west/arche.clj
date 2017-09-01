(ns troy-west.arche
  (:refer-clojure :exclude [resolve])
  (:require [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]
            [qbits.alia.codec.default :as codec.default])
  (:import (clojure.lang Keyword)))

(defprotocol StatementResolver
  (statement [this state]))

(extend-protocol StatementResolver
  Object
  (statement [this state] this)
  Keyword
  (statement [this state] (get-in state [:statements this])))

(defn prepare-statements
  [session statements]
  (reduce-kv (fn [ret k v]
               (assoc ret k (alia/prepare session v)))
             {}
             statements))

(defn prepare-encoders
  [session udts]
  (reduce-kv (fn [ret k v]
               (assoc ret k (alia.udt/encoder session
                                              (:name v)
                                              (or (:codec v) codec.default/codec))))
             {}
             udts))

(defn udt-encoder
  [connection key]
  (get-in connection [:udt-encoders key]))

(defn encode-udt
  [connection key value]
  (let [encoder (udt-encoder connection key)]
    (encoder value)))

(defn connect
  ([cluster]
   (connect cluster nil))
  ([cluster {:keys [keyspace statements udts]}]
   (let [session (if keyspace (alia/connect cluster keyspace)
                              (alia/connect cluster))]
     {:session      session
      :statements   (prepare-statements session (apply merge statements))
      :udt-encoders (prepare-encoders session (apply merge udts))})))

(defn disconnect
  [connection]
  (alia/shutdown (:session connection)))

(defn execute*
  ([f connection query]
   (f (:session connection) (statement connection query)))
  ([f connection query opts]
   (f (:session connection) (statement connection query) opts)))

(defn execute
  ([connection query]
   (execute* alia/execute connection query))
  ([connection query opts]
   (execute* alia/execute connection query opts)))

(defn execute-async
  ([connection query]
   (execute* alia/execute-async connection query))
  ([connection query opts]
   (execute* alia/execute-async connection query opts)))

