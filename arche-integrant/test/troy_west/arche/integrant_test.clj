(ns troy-west.arche.integrant-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche :as arche]
            [troy-west.arche.integrant]
            [integrant.core :as ig]
            [qbits.alia :as alia]))

(def cassandra-config
  {[:arche/cluster :test/cluster-1]       {:contact-points ["127.0.0.1"] :port 19142}
   [:arche/connection :test/connection-1] {:keyspace   "sandbox"
                                           :cluster    (ig/ref :test/cluster-1)
                                           :statements []
                                           :udts       {::asset {:name "asset"}}}
   [:arche/connection :test/connection-2] {:keyspace   "foobar"
                                           :cluster    (ig/ref :test/cluster-1)
                                           :statements []
                                           :udts       {::asset {:name "asset"}}}})

(deftest integrant-test

  (let [shutdowns (atom [])]

    (with-redefs [alia/cluster  (fn [_] ::cluster)
                  arche/connect (fn [_ opts] (keyword (str "connection-" (:keyspace opts))))
                  alia/shutdown (fn [x] (swap! shutdowns
                                               conj
                                               (keyword (str "shutdown-" (name x)))))]

      (let [init-comp (ig/init cassandra-config)]

        (is (= {[:arche/cluster :test/cluster-1]       ::cluster,
                [:arche/connection :test/connection-1] :connection-sandbox,
                [:arche/connection :test/connection-2] :connection-foobar}
               init-comp))

        (ig/halt! init-comp)

        (is (= @shutdowns [:shutdown-connection-foobar
                           :shutdown-connection-sandbox
                           :shutdown-cluster]))))))