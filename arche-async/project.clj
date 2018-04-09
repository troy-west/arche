(defproject com.troy-west/arche-async "0.4.1-SNAPSHOT"
  :description "Arche: Alia / core.async module"

  :url "https://github.com/troy-west/arche"
  
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[org.clojure/core.async "0.4.474"]
                 [com.troy-west/arche "_"]
                 [cc.qbits/alia-async "_" :exclusions [org.clojure/core.async]]])