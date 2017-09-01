# Arche: A Clojure Battery Pack for Alia/Cassandra

> [Arche](https://en.wikipedia.org/wiki/Arche_(mythology)): The ancient Greek muse of origins

Arche provides state management for Cassandra via [Alia](https://github.com/mpenet/alia).

## Summary

* Cassandra state management (Cluster / Session / Prepared Statements / Execution Options / UDTs)
* Optional DI/lifecycle via [Integrant](https://github.com/weavejester/integrant) or [Component](https://github.com/stuartsierra/component)
* Externalisation of query definitions via an extension of [HugSQL](https://github.com/layerware/hugsql) to support CQL
* Automatic hyphen/underscore translation with when using HugCQL
* Query configuration by simple EDN map of key/cql or key/map (when configuring per-query opts)
* Prepared statement execution by keyword, supports all Alia execution modes (vanilla, core.async, manifold)
* User Defined Type (UDT) encoding by keyword
* As much configuration from EDN as possible (see: tagged literals)

## Modules

* [com.troy-west.arche/arche](https://github.com/troy-west/arche/tree/master/arche)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche.svg)](https://clojars.org/com.troy-west/arche) [![CircleCI](https://circleci.com/gh/troy-west/arche.svg?style=svg)](https://circleci.com/gh/troy-west/arche)

  Cassandra state management, statement preperation and execution, and UDT encoding.

* [com.troy-west.arche/arche-hugcql](https://github.com/troy-west/arche/tree/master/arche-hugcql)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche-hugcql.svg)](https://clojars.org/com.troy-west/arche-hugcql)

  Externalise CQL definition and execution options in file/resource. Automatic hyphen/underscore translation.
  
* [com.troy-west.arche/arche-integrant](https://github.com/troy-west/arche/tree/master/arche-integrant)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche-integrant.svg)](https://clojars.org/com.troy-west/arche-integrant)

  Cassandra lifecycle and DI via [Integrant](https://github.com/weavejester/integrant)

* [com.troy-west.arche/arche-component](https://github.com/troy-west/arche/tree/master/arche-component)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche-component.svg)](https://clojars.org/com.troy-west/arche-component)

  Cassandra lifecycle and DI via [Component](https://github.com/stuartsierra/component)

* [com.troy-west.arche/arche-async](https://github.com/troy-west/arche/tree/master/arche-async)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche-async.svg)](https://clojars.org/com.troy-west/arche-async)

  Core.async statement execution (shadows alia.async)

* [com.troy-west.arche/arche-manifold](https://github.com/troy-west/arche/tree/master/arche-manifold)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche-manifold.svg)](https://clojars.org/com.troy-west/arche-manifold)

  Manifold statement execution (shadows alia.manifold)

## Usage

### Initial Setup

Define your schema (optionally, automate test cluster management with [CCM-CLJ](https://github.com/SMX-LTD/ccm-clj))

The tests and examples included with this project use the following:

```cql
CREATE TYPE asset (
    code     text,
    currency text,
    notional text);

CREATE TABLE trade (
    id           text,
    asset_basket map<text, frozen<asset>>,
    PRIMARY KEY (id));

CREATE TABLE client (
    id   text,
    name text,
    PRIMARY KEY (id));
```

### Externalise CQL Statements in HugsCQL files/resources

[Arche-HugCQL](https://github.com/troy-west/arche/tree/master/arche-hugcql) makes use of [HugSQL](https://www.hugsql.org/) to parse CQL statements externalised in files or resources. 

* --:name is converted into (an optionally namespaced) keyword that identifies this statement for execution.
* --:options is translated into EDN and applied as default execution configuration for this statement

Hyphens in select columns and named parameters are automatically translated by the [quoted identifier technique](https://stackoverflow.com/questions/20243562/clojure-variable-names-for-database-column-names/33259288#33259288).

e.g. The following HugCQL file:

```text
--:name test/insert-client
INSERT INTO client (id, name) VALUES (:id, :name)

--:name test/select-client
--:options {:fetch-size 500}
SELECT * FROM client WHERE id = :id

--:name test/insert-trade
INSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket)

--:name test/select-trade
SELECT id, :i:asset-basket FROM trade where id = :id
```

Translates to the following map of key -> statements:

```clojure
{:test/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
 :test/select-client {:cql  "SELECT * FROM client WHERE id = :id"
                      :opts {:fetch-size 500}}
 :test/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
 :test/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"}
```

After creating a connection, those statements can be executed by keyword

```clojure

(arche/execute connection
               :test/select-trade
               {:values {:id "id"}}))

=> [{:id           "some-id"
     :asset-basket {"long" {:code     "PB" ;; automatic underscore / hyphen translation of columns / keys
                            :currency "GBP"
                            :notional "12"}}}]
```

For convenience, a tagged literal is provided that translates file/resource paths to statements maps, e.g:

```text
#arche.hugcql/statements "stmts1.hcql"
```

### Vanilla State Management (no Component or Integrant)

Create a cluster instance using alia.

``` clojure
(def udts [{:test/asset {:name "asset"}}])

(def statements {:test/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
                 :test/select-client {:cql  "SELECT * FROM client WHERE id = :id"
                                      :opts {:fetch-size 500}}
                 :test/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
                 :test/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"})
```

Create an arche connection with the cluster and (optional) keyspace, statements, and UDT's.

``` clojure
(require '[troy-west.arche :as arche])

(def connection (arche/connect (alia/cluster {:contact-points ["127.0.0.1"] 
                                              :port 19142})
                               {:keyspace   "sandbox" 
                                :statements statements
                                :udts       [udts]}))
```

Using UDT encoders.

``` clojure
(arche/encode-udt connection :test/asset {:code     "AB"
                                          :currency "GBP"
                                          :notional "12"})

;; creates UDTValue instance
;; #object[com.datastax.driver.core.UDTValue 0x29632e50 "{code:'AB',currency:'GBP',notional:'12'}"]
```

Arche provides modules/functions that shadow all standard Alia execution, in modules that similarly shadow Alia. 

``` clojure
(arche/execute connection 
               :test/insert-client 
               {:values {:id "id-1" :name "Carol"}})

(arche.async/execute-chan connection 
                          :test/insert-client 
                          {:values {:id "id-1" :name "Carol"}})

(arche.manifold/execute connection 
                        :test/insert-client 
                        {:values {:id "id-1" :name "Carol"}})
```

### DI/Lifecycle management with [Integrant](https://github.com/weavejester/integrant) (recommended)

Create an Ingrant System with HugCQL externalised CQL prepared statements.

``` clojure
(def system
  (integrant/init 
    {:arche/cluster {:contact-points   ["127.0.0.1"] 
                     :port             19142}
     :arche/connection {:keyspace   "sandbox"
                        :cluster    (integrant/ref :arche/cluster)
                        :statements #arche.hugcql/statements "cql/test.hcql"
                        :udts       [{:arche/asset {:name "asset"}}]}}))

(def connection (:arche/connection system)

(arche/execute connection 
               :arche/insert-client
               {:values {:id "id-1" :name "Carol"}})

(arche.async/execute-chan connection 
                          :arche/select-client 
                          {:values {:id "id-1"}})

(arche.manifold/execute 
    connection
    :arche/insert-trade
    {:values {:id           "trade-1"
              :asset-basket {"long" (arche/encode-udt 
                                       connection
                                       :arche/asset
                                       {:code     "AB"
                                        :currency "GBP"
                                        :notional "12"})
                             "short" (arche/encode 
                                        connection 
                                        :arche/asset
                                        {:code     "ZX"
                                         :currency "AUD"
                                         :notional "98"})}}}))
```

### DI/Lifecycle managemet via [Component](https://github.com/stuartsierra/component) 

``` clojure
(def system
  (component/start-system
    {:cluster    #arche/cluster{:contact-points ["127.0.0.1"]
                                :port       19142}
     :connection #arche/connection{:keyspace   "sandbox"
                                   :statements [#arche.hugcql/statements "cql/test1.hcql"
                                                #arche.hugcql/statements "cql/test2.hcql"]
                                   :udts       [{:arche/asset {:name "asset"}}]
                                   :cluster    :cluster}}))

(def connection (:connection system)
```

## License

Copyright © 2017 [Troy-West, Pty Ltd.](http://www.troywest.com)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
