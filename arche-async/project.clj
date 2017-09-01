(defproject com.troy-west/arche-async "0.2.0"
  :description "Arche: Cassandra Clojure Cluster and Session Management with Alia"
  :url "http://www.troy-west.com/arche"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[com.troy-west/arche "_"]
                 [cc.qbits/alia-async "_"]]

  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies   [[com.smxemail/ccm-clj "1.1.0"]
                                    [ch.qos.logback/logback-classic "1.1.8"]]}})
