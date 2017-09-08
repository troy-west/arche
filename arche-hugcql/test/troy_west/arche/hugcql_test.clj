(ns troy-west.arche.hugcql-test
  (:require [clojure.test :refer [deftest is]]
            [troy-west.arche.hugcql :as hugcql]))

(deftest resolving

  (is (= ":id" (hugcql/resolve-value {:type :v :name :id})))
  (is (= ":\"id-one\"" (hugcql/resolve-value {:type :v :name :id-one})))

  (is (= "id" (hugcql/resolve-identifier {:type :i :name :id})))
  (is (= "id_one as \"id-one\"" (hugcql/resolve-identifier {:type :i :name :id-one})))

  (is (= ":id" (hugcql/resolve-key {:type :v :name :id})))
  (is (= ":\"id-one\"" (hugcql/resolve-key {:type :v :name :id-one})))

  (is (= "id" (hugcql/resolve-key {:type :i :name :id})))
  (is (= "id_one as \"id-one\"" (hugcql/resolve-key {:type :i :name :id-one}))))

(deftest statements

  (is (= {:arche/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
          :arche/select-client "SELECT * FROM client WHERE id = :id"
          :arche/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
          :arche/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"}
         (hugcql/statements
           [{:hdr {:name ["arche/insert-client"]}
             :sql ["INSERT INTO client (id, name) VALUES (" {:type :v :name :id} ", " {:type :v :name :name} ")"]}
            {:hdr {:name ["arche/select-client"]} :sql ["SELECT * FROM client WHERE id = " {:type :v :name :id}]}
            {:hdr {:name ["arche/insert-trade"]}
             :sql ["INSERT INTO trade (id, asset_basket) VALUES (" {:type :v :name :id} ", " {:type :v :name :asset-basket} ")"]}
            {:hdr {:name ["arche/select-trade"]}
             :sql ["SELECT id, " {:type :i :name :asset-basket} " FROM trade where id = " {:type :v :name :id}]}]))))

(deftest parse-statements

  (is (= {:arche/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"}
         (hugcql/parse "--:name arche/insert-client \nINSERT INTO client (id, name) VALUES (:id, :name)")))

  (is (= {:arche/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
          :arche/select-client "SELECT * FROM client WHERE id = :id"
          :arche/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
          :arche/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"}
         (hugcql/parse "--:name arche/insert-client \nINSERT INTO client (id, name) VALUES (:id, :name) \n--:name arche/select-client \nSELECT * FROM client WHERE id = :id \n--:name arche/insert-trade \nINSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket) \n--:name arche/select-trade \nSELECT id, :i:asset-basket FROM trade where id = :id")))

  (is (= {:arche/select-client "SELECT * FROM client WHERE id = :id and start > :\"date-time\" and end <= :\"date-time\" limit :limit"}
         (hugcql/parse "--:name arche/select-client \nSELECT * FROM client WHERE id = :id and start > :date-time and end <= :date-time limit :limit"))))

(deftest load-statements

  (is (= {:arche/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
          :arche/select-client {:cql  "SELECT * FROM client WHERE id = :id"
                                :opts {:fetch-size 500}}
          :arche/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
          :arche/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"}
         (hugcql/load "cql/test.hcql"))))

(deftest reader-literals

  (is (= {:arche/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
          :arche/select-client {:cql  "SELECT * FROM client WHERE id = :id"
                                :opts {:fetch-size 500}}
          :arche/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
          :arche/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"}
         #arche.hugcql/statements "cql/test.hcql")))

(deftest options-corner-cases

  ;; ok
  (is (= {:arche/select-client {:cql  "SELECT * FROM client WHERE id = :id"
                                :opts {:fetch-size 500}}}
         (hugcql/parse "--:name arche/select-client\n--:options {:fetch-size 500}\nSELECT * FROM client WHERE id = :id\n")))

  ;; invalid options
  (is (= {:arche/select-client "SELECT * FROM client WHERE id = :id"}
         (hugcql/parse "--:name arche/select-client\n--:options {:fetch-size}\nSELECT * FROM client WHERE id = :id\n")))

  (is (= {:arche/select-client {:cql  "SELECT * FROM client WHERE id = :id"
                                :opts {:one :two}}}
         (hugcql/parse "--:name arche/select-client\n--:options {:one #_ two :two}\nSELECT * FROM client WHERE id = :id\n")))

  ;; find one that fails, there's boud to be one..
  )