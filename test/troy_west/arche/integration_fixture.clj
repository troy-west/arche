(ns troy-west.arche.integration-fixture
  (:require [clojure.test :refer :all]
            [troy-west.arche :as arche]
            [troy-west.arche.spec :as arche.spec]
            [troy-west.arche.hugcql]
            [troy-west.arche.component]
            [troy-west.arche.integrant]
            [qbits.alia :as alia]
            [ccm-clj.core :as ccm]
            [integrant.core :as ig]
            [com.stuartsierra.component :as cp]))

(defonce system (atom {}))

(def udts [{:arche/asset {:name "asset"}}])

(def statements [#arche.hugcql/statements "cql/test1.hcql"
                 #arche.hugcql/statements "cql/test2.hcql"])

(defn start-system!
  []
  (ccm/auto-cluster! "arche"
                     "2.2.6"
                     3
                     [#"test-resources/cql/test-keyspace\.cql"]
                     {"sandbox" [#"test-resources/cql/test-tables\.cql"]}
                     19142)

  (let [hand-cluster     (alia/cluster {:contact-points ["127.0.0.1"]
                                        :port           19142})
        hand-connection  (arche/connect hand-cluster {:keyspace   "sandbox"
                                                      :statements #arche.hugcql/statements "cql/test.hcql"
                                                      :udts       udts})

        component-system (cp/start-system
                          {:cluster    #arche/cluster{:contact-points ["127.0.0.1"]
                                                      :port           19142}
                           :connection #arche/connection{:keyspace   "sandbox"
                                                         :statements [#arche.hugcql/statements "cql/test1.hcql"
                                                                      #arche.hugcql/statements "cql/test2.hcql"]
                                                         :udts       [{:arche/asset {:name "asset"}}]
                                                         :cluster    :cluster}})

        integrant-system (ig/init {:arche/cluster           {:contact-points ["127.0.0.1"] :port 19142}
                                          :arche/connection {:keyspace   "sandbox"
                                                             :cluster    (ig/ref :arche/cluster)
                                                             :statements statements
                                                             :udts       udts}})]

    (reset! system {"hand-rolled" {:cluster    hand-cluster
                                   :connection hand-connection}
                    "component"   {:connection (:connection component-system)
                                   :system     component-system}
                    "integrant"   {:connection (:arche/connection integrant-system)
                                   :system     integrant-system}})))

(defn connection
  [mode]
  (get-in @system [mode :connection]))

(defn stop-system!
  []
  (alia/shutdown (connection "hand-rolled"))
  (alia/shutdown (get-in @system ["hand-rolled" :cluster]))

  (cp/stop-system (get-in @system ["component" :system]))
  (ig/halt! (get-in @system ["integrant" :system]))

  (ccm/stop!)
  (reset! system {}))

(defn wrap-test
  [test-fn]
  (arche.spec/instrument!)
  (start-system!)
  (test-fn)
  (stop-system!))
