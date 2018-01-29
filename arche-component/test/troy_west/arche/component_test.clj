(ns troy-west.arche.component-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche.component :as arche.component]))

;; tagged literals provide a simple way to declare clusters and connections in EDN

(deftest tagged-literal-test

  (is (= #arche/cluster{:contact-points ["127.0.0.1"]
                        :port           19142}

         (arche.component/cluster {:contact-points ["127.0.0.1"]
                                   :port           19142})))

  (is (= #arche/connection{:keyspace   "sandbox"
                           :statements ["prepared/test.cql"]
                           :udts       {::asset {:name "asset"}}}

         (arche.component/connection {:keyspace   "sandbox"
                                      :statements ["prepared/test.cql"]
                                      :udts       {::asset {:name "asset"}}}))))

;; connections can optionally declare their cluster configuration

(deftest component-dependencies

  (is (= nil
         (meta
          (arche.component/connection {:keyspace   "sandbox"
                                       :statements ["prepared/test.cql"]
                                       :udts       {::asset {:name "asset"}}}))))

  (is (= {:com.stuartsierra.component/dependencies {:cluster :test/cluster}}
         (meta
          (arche.component/connection {:cluster    :test/cluster
                                       :keyspace   "sandbox"
                                       :statements ["prepared/test.cql"]
                                       :udts       {::asset {:name "asset"}}})))))