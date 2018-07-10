(defproject com.troy-west/arche-hugcql "0.4.4-SNAPSHOT"
  :description "Arche: prepared statement definition and externalisation via HugCQL"

  :url "https://github.com/troy-west/arche"
  
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v20.html"}

  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[com.layerware/hugsql "0.4.8"]]

  :dev {:resource-paths ["test-resources"]})