(defproject com.troy-west/arche-all "0.1.0-SNAPSHOT"
  :description "Arche: Cassandra Clojure Cluster and Session Management with Alia"
  :url "http://www.troy-west.com/arche"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-modules "0.3.11"]]
  :middleware [lein-modules.plugin/middleware]

  :profiles     {:dev {:source-paths   ["dev"]
                       :resource-paths ["test-resources"]
                       :dependencies   [[com.smxemail/ccm-clj "1.1.0"]
                                        [ch.qos.logback/logback-classic "1.1.8"]]}}
  :modules {:inherited {:dependencies [[org.clojure/clojure "_"]
                                       [cc.qbits/alia "_"]]
                        :resource-paths ["test-resources"]}
            :versions {org.clojure/clojure "1.8.0"
                       cc.qbits/alia "4.0.0-beta9"
                       com.troy-west/arche "0.1.0-SNAPSHOT"}})
