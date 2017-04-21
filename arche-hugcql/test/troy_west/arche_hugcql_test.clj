(ns troy-west.arche-hugcql-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [troy-west.arche-hugcql :as hugcql]))

(deftest test-prepared-statements
  (let [statements
        [{:arche/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
          :arche/select-client "SELECT * FROM client WHERE id = :id"
          :arche/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket)"
          :arche/select-trade  "SELECT id, :i:asset-basket FROM trade where id = :id"}

         "--:name arche/insert-client \nINSERT INTO client (id, name) VALUES (:id, :name) \n--:name arche/select-client \nSELECT * FROM client WHERE id = :id \n--:name arche/insert-trade \nINSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket) \n--:name arche/select-trade \nSELECT id, :i:asset-basket FROM trade where id = :id"

         ["prepared/test1.cql" "prepared/test2.cql"]]]

    (is (every?
         #(= {:arche/insert-client "INSERT INTO client (id, name) VALUES (:\"id\", :\"name\")"
              :arche/insert-trade "INSERT INTO trade (id, asset_basket) VALUES (:\"id\", :\"asset-basket\")"
              :arche/select-client "SELECT * FROM client WHERE id = :\"id\""
              :arche/select-trade "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :\"id\""}
             %)
         (map hugcql/prepared-statements statements)))))
