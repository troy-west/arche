(ns troy-west.arche-component-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche-component :as ac]
            [com.stuartsierra.component :as component]
            [troy-west.arche :as arche]
            [troy-west.arche-hugcql :as arche-hugcql]
            [qbits.alia :as alia]))

(def cassandra-config
  {:test/statements-1 #arche/statements["prepared/test.cql"]
   :test/udts-1       {::asset {:name "asset"}}
   :test/cluster-1    #arche/cluster{:contact-points ["127.0.0.1"] :port 19142}
   :test/session-1    #arche/session{:keyspace   "sandbox"
                                     :cluster    :test/cluster-1
                                     :statements :test/statements-1
                                     :udts       :test/udts-1}
   :test/session-2    #arche/session{:keyspace   "foobar"
                                     :cluster    :test/cluster-1
                                     :statements :test/statements-1
                                     :udts       :test/udts-1}})

(deftest compoment-test
  (let [shutdowns (atom [])]
    (with-redefs [alia/cluster                     (fn [_] ::cluster)
                  arche/init-session               (fn [x] (keyword (str "session-" (:keyspace x))))
                  arche-hugcql/prepared-statements (fn [_] ::statements)
                  alia/shutdown                    (fn [x] (swap! shutdowns
                                                                  conj
                                                                  (keyword (str "shutdown-" (name x)))))]

      (is (instance? troy_west.arche_component.StatementsComponent (:test/statements-1 cassandra-config)))
      (is (instance? troy_west.arche_component.ClusterComponent (:test/cluster-1 cassandra-config)))
      (is (instance? troy_west.arche_component.SessionComponent (:test/session-1 cassandra-config)))
      (is (instance? troy_west.arche_component.SessionComponent (:test/session-2 cassandra-config)))

      (let [init-comp (component/start (component/map->SystemMap cassandra-config))]
        (is (= {:test/cluster-1 #troy_west.arche_component.ClusterComponent
                {:cluster ::cluster,
                 :config {:contact-points ["127.0.0.1"], :port 19142}},
                :test/session-1 #troy_west.arche_component.SessionComponent
                {:cluster #troy_west.arche_component.ClusterComponent
                 {:cluster ::cluster,
                  :config {:contact-points ["127.0.0.1"], :port 19142}},
                 :keyspace "sandbox",
                 :session :session-sandbox,
                 :statements ::statements,
                 :udts {::asset {:name "asset"}}},
                :test/session-2 #troy_west.arche_component.SessionComponent
                {:cluster #troy_west.arche_component.ClusterComponent
                 {:cluster ::cluster,
                  :config {:contact-points ["127.0.0.1"], :port 19142}},
                 :keyspace "foobar",
                 :session :session-foobar,
                 :statements ::statements,
                 :udts {::asset {:name "asset"}}},
                :test/statements-1 ::statements,
                :test/udts-1 {::asset {:name "asset"}}}
               (into {} init-comp)))

        (is (= (ac/session init-comp :test/session-1) :session-sandbox)
            (= (ac/session init-comp :test/session-2) :session-foobar))

        (let [stop-comp (component/stop init-comp)]

          (is (nil? (-> stop-comp :test/session-1 :session)))
          (is (nil? (-> stop-comp :test/session-2 :session)))

          (is (= @shutdowns [:shutdown-session-foobar
                             :shutdown-session-sandbox
                             :shutdown-cluster])))))))
