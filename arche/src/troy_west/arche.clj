(ns troy-west.arche
  (:refer-clojure :exclude [resolve])
  (:require [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]
            [qbits.alia.codec.default :as codec.default]))

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
  ([cluster {:keys [keyspace statements udts]}]
   (let [session (if keyspace
                   (alia/connect cluster keyspace)
                   (alia/connect cluster))]
     {:session      session
      :statements   (prepare-statements session (if (map? statements)
                                                  statements
                                                  (apply merge statements)))
      :udt-encoders (prepare-encoders session (if (map? udts)
                                                udts
                                                (apply merge udts)))})))

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

