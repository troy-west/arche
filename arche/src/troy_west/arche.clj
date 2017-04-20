(ns troy-west.arche
  (:require [qbits.alia :as alia]
            [qbits.alia.codec.default :as codec.default]
            [qbits.alia.udt :as alia.udt])
  (:import (com.datastax.driver.core Session
                                     BoundStatement
                                     SimpleStatement)))

(definterface IBindable
  (^com.datastax.driver.core.BoundStatement bind [statement-store]))

(defn bindable-statement
  [statement-key values codec]
  (let [q            ""
        ;; hack since ealier versions don't have a way to get paging state
        paging-state (atom nil)]
    (proxy [SimpleStatement IBindable] [q]
      (bind [statement-store]
        (let [statement    (alia/bind (get statement-store statement-key)
                                      values
                                      codec)]
          (alia/set-statement-options! statement
                                       (.getRoutingKey this nil nil)
                                       (.getRetryPolicy this)
                                       (.isTracing this)
                                       (.isIdempotent this)
                                       (some-> (.getConsistencyLevel this)
                                               str clojure.string/lower-case keyword)
                                       (some-> (.getSerialConsistencyLevel this)
                                               str clojure.string/lower-case keyword)
                                       (.getFetchSize this)
                                       (.getDefaultTimestamp this)
                                       @paging-state
                                       (let [timeout (.getReadTimeoutMillis this)]
                                         (when (> timeout 0) timeout)))
          statement))
      (setPagingState [ps] (reset! paging-state ps))
      (getQueryString [] ""))))

(extend-protocol alia/PStatement
  clojure.lang.Keyword
  (query->statement [q values codec]
    (bindable-statement q values codec)))

(defprotocol IExecutable
  (build-executable [this statement-store]))

(extend-protocol IExecutable
  IBindable
  (build-executable [this statement-store]
    (.bind this statement-store))

  Object
  (build-executable [this _]
    this))

(defn wrap-session
  [session {:keys [statements codecs]
            :or {statements {}
                 codecs     {}} :as state}]
  (proxy [Session clojure.lang.ILookup] []
    (close [] (.close session))
    (closeAsync [] (.closeAsync session))
    (execute
      ([q] (.execute session (build-executable q statements)))
      ([q vs] (.execute session q vs))
      ([q v & vs] (.execute session q (conj vs v))))
    (executeAsync
      ([q] (.executeAsync session (build-executable q statements)))
      ([q vs] (.executeAsync session q vs))
      ([q v & vs] (.executeAsync session q (conj vs v))))
    (getCluster [] (.getCluster session))
    (getLoggedKeyspace [] (.getLoggedKeyspace session))
    (getState [] (.getState session))
    (init [] (.init session))
    (initAsync [] (.initAsync session))
    (isClosed [] (.isClosed session))
    (prepare [q] (.prepare session q))
    (prepareAsync [q] (.prepareAsync q))

    (valAt [k] (.valAt state k))))

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

(defn init-session
  "Initialise a new session binding prepared statements and udts to the session."
  [{:keys [keyspace cluster statements udts] :as config}]
  (let [session             (if keyspace
                              (alia/connect cluster keyspace)
                              (alia/connect cluster))
        prepared-statements (prepare-statements session statements)
        prepared-udts       (prepare-encoders session udts)]
    (wrap-session session
                  (merge config
                         {:statements prepared-statements
                          :udts       prepared-udts}))))

(defn encode
  [session udts-key value]
  ((-> session :udts udts-key) value))

(defn connect
  ([cluster keyspace {:keys [statements udts]}]
   (init-session {:cluster    cluster
                  :keyspace   keyspace
                  :statements statements
                  :udts       udts}))
  ([cluster options]
   (connect cluster nil options)))
