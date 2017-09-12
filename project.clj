(defproject com.troy-west/arche-all "0.2.2-SNAPSHOT"
  :description "Arche: A Clojure Battery Pack for Cassandra/Alia"
  :url "http://www.troy-west.com/arche"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-modules "0.3.11"]
            [lein-cljfmt "0.5.7"]]

  :dependencies [[org.clojure/clojure "_"]
                 [cc.qbits/alia "_"]
                 [com.troy-west/arche "_"]
                 [com.troy-west/arche-hugcql "_"]
                 [com.troy-west/arche-integrant "_"]
                 [com.troy-west/arche-component "_"]
                 [com.troy-west/arche-async "_"]
                 [com.troy-west/arche-manifold "_"]
                 [clojure-future-spec "_"]]

  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies   [[com.smxemail/ccm-clj "1.1.0"]
                                    [ch.qos.logback/logback-classic "1.1.8"]]}}

  :modules {:inherited {:dependencies        [[org.clojure/clojure "_"]
                                              [cc.qbits/alia "_"]]
                        :subprocess          nil
                        :deploy-repositories [["releases" {:url "https://clojars.org/repo/" :creds :gpg}]]}
            :versions  {org.clojure/clojure           "1.8.0"
                        clojure-future-spec           "1.9.0-alpha17"
                        cc.qbits/alia                 "4.0.0"
                        cc.qbits/alia-async           "4.0.0"
                        cc.qbits/alia-manifold        "4.0.0"
                        com.troy-west/arche           :version
                        com.troy-west/arche-hugcql    :version
                        com.troy-west/arche-integrant :version
                        com.troy-west/arche-component :version
                        com.troy-west/arche-async     :version
                        com.troy-west/arche-manifold  :version}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["modules" "change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["modules" "deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["modules" "change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
