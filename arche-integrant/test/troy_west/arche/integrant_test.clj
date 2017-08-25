(ns troy-west.arche.integrant-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche :as arche]
            [troy-west.arche.integrant :as arche.integrant]
            [integrant.core :as integrant]
            [qbits.alia :as alia]))

(def cassandra-config
  {[:cassandra/cluster :test/cluster-1]       {:contact-points ["127.0.0.1"] :port 19142}
   [:cassandra/connection :test/connection-1] {:keyspace   "sandbox"
                                               :cluster    (integrant/ref :test/cluster-1)
                                               :statements []
                                               :udts       {::asset {:name "asset"}}}
   [:cassandra/connection :test/connection-2] {:keyspace   "foobar"
                                               :cluster    (integrant/ref :test/cluster-1)
                                               :statements []
                                               :udts       {::asset {:name "asset"}}}})

(deftest ^:integration integrant-test
  (let [shutdowns (atom [])]

    (with-redefs [alia/cluster  (fn [_] ::cluster)
                  arche/connect (fn [_ opts] (keyword (str "connection-" (:keyspace opts))))
                  alia/shutdown (fn [x] (swap! shutdowns
                                               conj
                                               (keyword (str "shutdown-" (name x)))))]

      (let [init-comp (integrant/init cassandra-config)]
        (is (= {[:cassandra/cluster :test/cluster-1]       ::cluster,
                [:cassandra/connection :test/connection-1] :connection-sandbox,
                [:cassandra/connection :test/connection-2] :connection-foobar}
               init-comp))

        (let [stop-comp (integrant/halt! init-comp)]
          (is (= @shutdowns [:shutdown-connection-foobar
                             :shutdown-connection-sandbox
                             :shutdown-cluster])))))))
