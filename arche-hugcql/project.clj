(defproject com.troy-west/arche-hugcql "0.3.6-SNAPSHOT"
  :description "Arche: HugCQL module"

  :url "https://github.com/troy-west/arche"
  
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}


  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[com.layerware/hugsql "0.4.8"]]

  :dev {:resource-paths ["test-resources"]})
