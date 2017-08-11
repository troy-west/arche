(ns troy-west.arche
  (:require [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]
            [qbits.alia.codec.default :as codec.default]))

(defn prepare-statements
  [connection statements]
  (reduce-kv (fn [ret k v]
               (assoc ret k (alia/prepare connection v)))
             {}
             statements))

(defn prepare-encoders
  [connection udts]
  (reduce-kv (fn [ret k v]
               (assoc ret k (alia.udt/encoder connection
                                              (:name v)
                                              (or (:codec v) codec.default/codec))))
             {}
             udts))

(defn statement
  [connection key]
  (get-in connection [:statements key]))

(defn udt-encoder
  [connection key]
  (get-in connection [:udt-encoders key]))

(defn encode-udt
  [connection key value]
  (let [encoder (udt-encoder connection key)]
    (encoder value)))

(defn connect
  [cluster {:keys [keyspace statements udts mode]}]
  (let [session (if keyspace (alia/connect cluster keyspace)
                             (alia/connect cluster))]
    {:session      session
     :statements   (prepare-statements session statements)
     :udt-encoders (prepare-encoders session udts)}))

;; standard
;; execute / execute-async
(defn execute
  ([connection query]
   (execute connection query nil))
  ([connection query opts]
   (alia/execute (:session connection)
                 (or (statement connection query) query)
                 opts)))

(defn execute-async
  ([connection query]
   (execute connection query nil))
  ([connection query opts]
   (alia/execute-async (:session connection)
                       (or (statement connection query) query)
                       opts)))