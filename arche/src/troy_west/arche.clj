(ns troy-west.arche
  (:refer-clojure :exclude [derive])
  (:require [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]
            [qbits.alia.enum :as alia.enum]
            [qbits.alia.codec.default :as codec.default])
  (:import (com.datastax.driver.core Cluster BatchStatement)))

(defn prepare-statements
  [session config]
  (reduce-kv (fn [ret k v]
               (let [cql      (or (:cql v) v)
                     opts     (:opts v)
                     prepared (alia/prepare session cql)]
                 (assoc ret k (cond-> {:cql      cql
                                       :prepared prepared}
                                opts (assoc :opts opts)))))
             {}
             config))

(defn prepare-encoders
  [session udts]
  (reduce-kv (fn [ret k v]
               (assoc ret k (alia.udt/encoder session
                                              (:name v)
                                              (or (:codec v) codec.default/codec))))
             {}
             udts))

(defn connect
  ([cluster]
   (connect cluster nil))
  ([^Cluster cluster {:keys [keyspace statements udts]}]
   (let [session    (if keyspace
                      (alia/connect cluster keyspace)
                      (alia/connect cluster))
         statements (if (map? statements) statements (apply merge statements))
         udts       (if (map? udts) udts (apply merge udts))]
     {:session      session
      :statements   (prepare-statements session statements)
      :udt-encoders (prepare-encoders session udts)
      :fetch-size   (-> cluster .getConfiguration .getQueryOptions .getFetchSize)})))

(defn derive
  "Derive a new connection from an existing one"
  [connection {:keys [statements udts]}]
  (let [{:keys [session fetch-size]} connection
        statements (if (map? statements) statements (apply merge statements))
        udts       (if (map? udts) udts (apply merge udts))]
    {:session      session
     :statements   (prepare-statements session statements)
     :udt-encoders (prepare-encoders session udts)
     :fetch-size   fetch-size}))

(defn disconnect
  [connection]
  (alia/shutdown (:session connection)))

(defn encode-udt
  [connection key value]
  (let [encoder (get-in connection [:udt-encoders key])]
    (encoder value)))

(defn options
  [connection key opts]
  (or (some-> (get-in connection [:statements key :opts])
              (merge opts))
      opts))

(defn batch
  ([connection key values-seq]
   (batch connection key values-seq :unlogged))
  ([connection key values-seq type]
   (let [bs (BatchStatement. (alia.enum/batch-statement-type type))]
     (doseq [values values-seq]
       (let [stmt (get-in connection [:statements key :prepared])]
         (.add bs (alia/bind stmt values))))
     bs)))

(defn execute*
  ([f connection key]
   (execute* f connection key nil))
  ([f connection key opts]
   (f (:session connection)
      (get-in connection [:statements key :prepared])
      (options connection key opts))))

(defn execute
  ([connection key]
   (execute* alia/execute connection key))
  ([connection key opts]
   (execute* alia/execute connection key opts)))

(defn execute-async
  ([connection key]
   (execute* alia/execute-async connection key))
  ([connection key opts]
   (execute* alia/execute-async connection key opts)))

