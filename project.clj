(defproject com.troy-west/arche-all "0.4.6-SNAPSHOT"
  :description "Arche: A Clojure Battery Pack for Cassandra/Alia"

  :url "https://github.com/troy-west/arche"

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v20.html"}

  :plugins [[lein-modules "0.3.11"]
            [lein-cljfmt "0.6.4" :exclusions [org.clojure/clojure]]
            [lein-kibit "0.1.6" :exclusions [org.clojure/clojure org.clojure/tools.reader]]]

  :dependencies [[org.clojure/clojure "_"]
                 [cc.qbits/alia "_"]
                 [com.troy-west/arche "_"]
                 [com.troy-west/arche-hugcql "_"]
                 [com.troy-west/arche-integrant "_"]
                 [com.troy-west/arche-component "_"]
                 [com.troy-west/arche-async "_"]
                 [com.troy-west/arche-manifold "_"]]

  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies   [[com.smxemail/ccm-clj "1.1.0"]
                                    [ch.qos.logback/logback-classic "1.2.3"]]}}

  :modules {:inherited {:dependencies        [[org.clojure/clojure "_"]]

                        :subprocess          nil

                        :deploy-repositories [["releases" {:url "https://clojars.org/repo/" :creds :gpg}]]

                        :aliases             {"puff" ["do"
                                                      ["clean"]
                                                      ["install"]
                                                      ["deps"]
                                                      ["check"]
                                                      ["test"]
                                                      ["kibit"]
                                                      ["cljfmt" "check"]]}}

            :versions  {org.clojure/clojure           "1.10.1"
                        cc.qbits/alia                 "4.1.1"
                        cc.qbits/alia-async           "4.1.1"
                        cc.qbits/alia-manifold        "4.1.1"
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
                  ["vcs" "push"]]

  :aliases {"smoke" ["do" ["modules" "puff"] ["clean"] ["check"] ["kibit"] ["cljfmt" "check"]]}

  :pedantic? :abort)
