(ns troy-west.arche.component-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche :as arche]
            [troy-west.arche.component :as ac]
            [troy-west.arche-hugcql :as arche-hugcql]
            [com.stuartsierra.component :as component]
            [qbits.alia :as alia])
  (:import (com.datastax.driver.core Cluster)
           (troy_west.arche.component Connection Statements)))

(def cassandra-config
  {:test/statements-1 #arche/statements["prepared/test.cql"]
   :test/udts-1       {::asset {:name "asset"}}
   :test/cluster-1    #arche/cluster{:contact-points ["127.0.0.1"] :port 19142}
   :test/session-1    #arche/connection{:keyspace   "sandbox"
                                        :cluster    :test/cluster-1
                                        :statements :test/statements-1
                                        :udts       :test/udts-1}
   :test/session-2    #arche/connection{:keyspace   "foobar"
                                        :cluster    :test/cluster-1
                                        :statements :test/statements-1
                                        :udts       :test/udts-1}})

(deftest component-test
  (let [shutdowns (atom [])]
    (with-redefs [alia/cluster                     (fn [_] ::cluster)
                  arche/connect                    (fn [_ opts] (keyword (str "session-" (:keyspace opts))))
                  arche-hugcql/prepared-statements (fn [_] ::statements)
                  alia/shutdown                    (fn [x] (swap! shutdowns
                                                                  conj
                                                                  (keyword (str "shutdown-" (name x)))))]

      (is (instance? Statements (:test/statements-1 cassandra-config)))
      (is (instance? Cluster (:test/cluster-1 cassandra-config)))
      (is (instance? Connection (:test/session-1 cassandra-config)))
      (is (instance? Connection (:test/session-2 cassandra-config)))

      (let [init-comp (component/start (component/map->SystemMap cassandra-config))
            cluster   (:test/cluster-1 init-comp)]

        (is (= {:test/cluster-1    cluster
                :test/session-1    #Connection
                                       {:cluster    cluster
                                        :keyspace   "sandbox",
                                        :session    :session-sandbox,
                                        :statements ::statements,
                                        :udts       {::asset {:name "asset"}}},
                :test/session-2    #Connection
                                       {:cluster    cluster
                                        :keyspace   "foobar",
                                        :session    :session-foobar,
                                        :statements ::statements,
                                        :udts       {::asset {:name "asset"}}},
                :test/statements-1 ::statements,
                :test/udts-1       {::asset {:name "asset"}}}
               (into {} init-comp)))

        (is (= (ac/connection init-comp :test/session-1) :session-sandbox)
            (= (ac/connection init-comp :test/session-2) :session-foobar))

        (let [stop-comp (component/stop init-comp)]

          (is (nil? (-> stop-comp :test/session-1 :session)))
          (is (nil? (-> stop-comp :test/session-2 :session)))

          (is (= @shutdowns [:shutdown-session-foobar
                             :shutdown-session-sandbox
                             :shutdown-cluster])))))))
