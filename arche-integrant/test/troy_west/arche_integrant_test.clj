(ns troy-west.arche-integrant-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche-integrant :as ai]
            [integrant.core :as ig]
            [troy-west.arche :as arche]
            [troy-west.arche-hugcql :as arche-hugcql]
            [qbits.alia :as alia]))

(def cassandra-config
  {[:arche/statements :test/statements-1] ["prepared/test.cql"]
   [:arche/udts :test/udts-1]             {::asset {:name "asset"}}
   [:cassandra/cluster :test/cluster-1]   {:contact-points ["127.0.0.1"] :port 19142}
   [:cassandra/session :test/session-1]   {:keyspace   "sandbox"
                                           :cluster    (ig/ref :test/cluster-1)
                                           :statements (ig/ref :test/statements-1)
                                           :udts       (ig/ref :test/udts-1)}
   [:cassandra/session :test/session-2]   {:keyspace   "foobar"
                                           :cluster    (ig/ref :test/cluster-1)
                                           :statements (ig/ref :test/statements-1)
                                           :udts       (ig/ref :test/udts-1)}})

(deftest ^:integration component-test
  (let [shutdowns (atom [])]
    (with-redefs [alia/cluster                     (fn [_] ::cluster)
                  arche/init-session               (fn [x] (keyword (str "session-" (:keyspace x))))
                  arche-hugcql/prepared-statements (fn [_] ::statements)
                  alia/shutdown                    (fn [x] (swap! shutdowns
                                                                  conj
                                                                  (keyword (str "shutdown-" (name x)))))]
      (let [init-comp (ig/init cassandra-config)]
        (is (= {[:arche/statements :test/statements-1] ::statements,
                [:arche/udts :test/udts-1]             {::asset {:name "asset"}},
                [:cassandra/cluster :test/cluster-1]   ::cluster,
                [:cassandra/session :test/session-1]   :session-sandbox,
                [:cassandra/session :test/session-2]   :session-foobar}
               init-comp))

        (is (= (ai/session init-comp :test/session-1) :session-sandbox)
            (= (ai/session init-comp :test/session-2) :session-foobar))

        (let [stop-comp (ig/halt! init-comp)]
          (is (= @shutdowns [:shutdown-session-foobar
                             :shutdown-session-sandbox
                             :shutdown-cluster])))))))
