(defproject com.troy-west/arche-integrant "0.1.2-SNAPSHOT"
  :description "Arche system configuration using the the Integrant library."

  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[com.troy-west/arche "_"]
                 [com.troy-west/arche-hugcql "_"]
                 [integrant "0.3.3"]]

  :test-selectors {:default (complement :integration)})
