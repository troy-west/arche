(ns troy-west.arche
  (:refer-clojure :exclude [derive resolve])
  (:require [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]
            [qbits.alia.enum :as alia.enum]
            [qbits.alia.codec.default :as codec.default])
  (:import (com.datastax.driver.core Cluster BatchStatement)
           (clojure.lang Keyword)))

(defprotocol StatementResolver
  (resolve [this connection opts]))

(extend-protocol StatementResolver

  Object
  (resolve [this _ _] this)

  Keyword
  (resolve [this connection _]
    (get-in connection [:statements this :prepared])))

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
  [connection udt-key value]
  (let [encoder (get-in connection [:udt-encoders udt-key])]
    (encoder value)))

(defn options
  [connection stmt-key opts]
  (or (some-> (get-in connection [:statements stmt-key :opts])
              (merge opts))
      opts))

(defn batch
  ([connection stmt-key values-seq]
   (batch connection stmt-key values-seq :unlogged))
  ([connection stmt-key values-seq type]
   (let [bs (BatchStatement. (alia.enum/batch-statement-type type))]
     (doseq [values values-seq]
       (let [stmt (get-in connection [:statements stmt-key :prepared])]
         (.add bs (alia/bind stmt values))))
     bs)))

(defn execute*
  ([f connection executable]
   (execute* f connection executable nil))
  ([f connection executable opts]
   (f (:session connection)
      (resolve executable connection opts)
      (options connection executable opts))))

(defn execute
  ([connection executable]
   (execute* alia/execute connection executable))
  ([connection executable opts]
   (execute* alia/execute connection executable opts)))

(defn execute-async
  ([connection executable]
   (execute* alia/execute-async connection executable))
  ([connection executable opts]
   (execute* alia/execute-async connection executable opts)))

