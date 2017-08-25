# Arche: A Clojure Battery Pack for Cassandra and Alia

> [Arche](https://en.wikipedia.org/wiki/Arche_(mythology)): The ancient Greek muse of origins

## Summary

Arche provides:

* DI/Lifecycle management of Cassandra state (cluster/session/statement/UDT) via [Integrant](https://github.com/weavejester/integrant) or [Component](https://github.com/stuartsierra/component)
* Externalisation of prepared statement configuration via a CQL extension of [HugSQL](https://github.com/layerware/hugsql)
* Seamless support for existing [Alia](https://github.com/mpenet/alia) execution
* Execution of prepared statements by keyword
* Encoding of UDT types by keyword
* As much configuration from EDN as possible

## Modules

* [com.troy-west.arche/arche](https://github.com/troy-west/arche/tree/master/arche)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche.svg)](https://clojars.org/com.troy-west/arche) [![CircleCI](https://circleci.com/gh/troy-west/arche.svg?style=svg)](https://circleci.com/gh/troy-west/arche)

  Cassandra state management, statement preperation and execution, and UDT encoding.

* [com.troy-west.arche/arche-hugcql](https://github.com/troy-west/arche/tree/master/arche-hugcql)

  [![Clojars Project](https://img.shields.io/clojars/v/com.troy-west/arche-hugcql.svg)](https://clojars.org/com.troy-west/arche-hugcql)

  Parse CQL statements from file or resource in HugsSQL format, provides automatic hyphen/underscore translation.

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

  Manifold statement execution (shadows alia.async)

## Usage

### Initial Setup

Define your schema (optionally, automate test cluster management with [CCM-CLJ](https://github.com/SMX-LTD/ccm-clj))

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

### Externalise CQL Statements in HugsCQL Files/Resources

[Arche-HugCQL](https://github.com/troy-west/arche/tree/master/arche-hugcql) makes use of [HugSQL](https://www.hugsql.org/) to parse CQL statements externalised in files or resources. 

The --:name field is converted into (an optionally namespaced) keyword that identifies this statement for execution.

Hyphens in select columns and named parameters are automatically translated by the [quoted identifier technique](https://stackoverflow.com/questions/20243562/clojure-variable-names-for-database-column-names/33259288#33259288).

e.g. The following HugCQL file:

```text
--:name test/insert-client
INSERT INTO client (id, name) VALUES (:id, :name)

--:name test/select-client
SELECT * FROM client WHERE id = :id

--:name test/insert-trade
INSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket)

--:name test/select-trade
SELECT id, :i:asset-basket FROM trade where id = :id
```

Translates to the following map of key -> statements:

```clojure
{:test/insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
 :test/select-client "SELECT * FROM client WHERE id = :id"
 :test/insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
 :test/select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"}
```

Note the quoted identifier for asset-basket in test/insert-trade and test/select-trade, this allows you insert and select maps of data with kebab-case keywords:

```clojure

;; in this particular schema the asset is a UDT, more on udt encoders below.
(let [encoded-udt (arche/encode-udt connection
                                    :arche/asset
                                    {:code     "PB"
                                     :currency "GBP"
                                     :notional "12"})]
  (arche/execute connection 
                 :test/insert-trade 
                 {:values {:id           "some-id"
                           :asset-basket {"pork-bellies" encoded-udt}}}})

  (arche/execute connection
                 :test/select-trade
                 {:values {:id "id"}}))

=> [{:id           "some-id"
     :asset-basket {"pork-bellies" {:code     "PB"
                                    :currency "GBP"
                                    :notional "12"}}}]
```
### Usage without Integrant or Component

Create a cluster instance using alia.

```clojure
(require '[qbits.alia :as alia])

(def cluster (alia/cluster {:contact-points ["127.0.0.1"] :port 19142}))
```

Define some statements and UDT's.

``` clojure
(def udts {::asset {:name "asset"}})

(def statements {::insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
                 ::select-client "SELECT * FROM client WHERE id = :id"
                 ::insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
                 ::select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"})
```

Create an arche session with the cluster and the sandbox keyspace and pass the statements and UDT's as options.

``` clojure
(require '[troy-west.arche :as arche])

(def session (arche/connect cluster "sandbox" {:statements statements :udts udts}))
```

Use new UDT encoders.

``` clojure
(arche/encode session ::asset {:code     "AB"
                               :currency "GBP"
                               :notional "12"})

;; creates UDTValue instance
;; #object[com.datastax.driver.core.UDTValue 0x29632e50 "{code:'AB',currency:'GBP',notional:'12'}"]
```

Use alia execute queries against the session.

``` clojure
(alia/execute session ::insert-client {:values {:id "id-1" :name "Carol"}})

(alia/execute-chan session ::insert-client {:values {:id "id-1" :name "Carol"}})

(alia/execute-async session ::insert-client {:values {:id "id-1" :name "Carol"}})
```

### Parsing CQL prepared statements

Arche-hugcql makes use of [HugSQL](https://www.hugsql.org/) to parse CQL prepared statements defined in CQL files, Strings or in a clojure map. It supports HugSQL [value parameters](https://www.hugsql.org/#param-value) and [identifier parameters](https://www.hugsql.org/#param-identifier). You can include the keywords in your CQL queries and the names will be properly quoted for you, automatically handling kebab cased keywords.

i.e. these keywords will translate as follows
``` text
:my-value                 => :\"my-value\"
:v:my-value               => :\"my-value\"
:value:my-value           => :\"my-value\"
:i:my-identifier          => my_identifier as "\my-identifier\"
:identifier:my-identifier => my_identifier as "\my-identifier\"
```

The cql parsing is all done through the `prepared-statements` function. This function produces a map of prepared statement Strings with the identifiers and values quoted.

#### maps example

``` clojure
(require '[troy-west.arche-hugcql :as arche-hugcql])

(arche-hugcql/prepared-statements {:foo/bar "select :i:foo-bar from emp as where id_num = :id-num"})
;; => {:foo/bar "select foo_bar as \"foo-bar\" from emp as where id_num = :\"id-num\""}
```

#### Strings example

``` clojure
(arche-hugcql/prepared-statements "--:name foo/bar \nselect :i:foo-bar from emp where id_num = :id-num")
;; => {:foo/bar "select foo_bar as \"foo-bar\" from emp as where id_num = :\"id-num\""}
```

#### Files example

Create a file in the resources directory called foo_bar.cql

``` text
--:name foo/bar
select :i:foo-bar from emp where id_num = :id-num
```

Then cql file can be loaded like this:

``` clojure
(arche-hugcql/prepared-statements ["foo_bar.cql"])
;; => {:foo/bar "select foo_bar as \"foo-bar\" from emp as where id_num = :\"id-num\""}
```

### [Integrant](https://github.com/weavejester/integrant) DI/Lifecycle Management (recommended)

Create prepared statements cql in `prepared/test.cql`

``` text
--:name arche/insert-client
INSERT INTO client (id, name) VALUES (:id, :name)

--:name arche/select-client
SELECT * FROM client WHERE id = :id

--:name arche/insert-trade
INSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket)

--:name arche/select-trade
SELECT id, :i:asset-basket FROM trade where id = :id
```

Define the integrant cassandra configuration, can either be defined in a text file or in code.

Note: The `ig/ref` calls can be replaced by integrant `#ref` tag to allow the config to be defined declaratively as EDN data.

``` clojure
(require '[integrant.core :as ig])
(require '[troy-west.arche-integrant :as arche])

(def cassandra-config
  {[:arche/statements :test/statements-1] ["prepared/test.cql"]
   [:arche/udts :test/udts-1]             {::asset {:name "asset"}}
   [:arche/cluster :test/cluster-1]   {:contact-points ["127.0.0.1"] :port 19142}
   [:arche/session :test/session-1]   {:keyspace   "sandbox"
                                       :cluster    (ig/ref :test/cluster-1)
                                       :statements (ig/ref :test/statements-1)
                                       :udts       (ig/ref :test/udts-1)}})
```

Start the cassandra component.

``` clojure
(def cassandra (ig/init cassandra-config))
```

Execute some prepared statements using alia.

``` clojure
(require '[qbits.alia :as alia])

(let [session (arche/session cassandra :test/session-1)]
  (alia/execute session :arche/insert-client {:values {:id "id-1" :name "Carol"}})
  (alia/execute session :arche/select-client {:values {:id "id-1"}})
  (alia/execute session :arche/insert-trade
    {:values {:id "trade-1"
              :asset-basket {"long" (arche/encode session
                                                  ::asset
                                                  {:code     "AB"
                                                   :currency "GBP"
                                                   :notional "12"})
                             "short" (arche/encode session
                                                   ::asset
                                                   {:code     "ZX"
                                                    :currency "AUD"
                                                    :notional "98"})}}}))
```

### [Component](https://github.com/stuartsierra/component) DI/Lifecycle Management

Create prepared statements cql in `prepared/test.cql`

``` text
--:name arche/insert-client
INSERT INTO client (id, name) VALUES (:id, :name)

--:name arche/select-client
SELECT * FROM client WHERE id = :id

--:name arche/insert-trade
INSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket)

--:name arche/select-trade
SELECT id, :i:asset-basket FROM trade where id = :id
```

Define the Component cassandra configuration, can either be defined in a text file or in code.

Note: the data reader tags #arche/statements, #arche/cluster and #arche/session are made avalible by the `arche-component` library to make it possible to declaratively define the Component configuration as EDN data. Alternatively there are functions to achieve the same, `create-statements`, `create-cluster` and `create-session`. There will create arche Component Records.

``` clojure
(require '[com.stuartsierra.component :as component])
(require '[troy-west.arche-component :as arche])

(def cassandra-config
  {:test/statements-1 #arche/statements["prepared/test.cql"]
   :test/udts-1       {::asset {:name "asset"}}
   :test/cluster-1    #arche/cluster{:contact-points ["127.0.0.1"] :port 19142}
   :test/session-1    #arche/session{:keyspace   "sandbox"
                                     :cluster    :test/cluster-1
                                     :statements :test/statements-1
                                     :udts       :test/udts-1}})
```

Start the cassandra component.

``` clojure
(def cassandra (component/start (component/map->SystemMap cassandra-config)))
```

Execute some prepared statements using alia.

``` clojure
(require '[qbits.alia :as alia])

(let [session (arche/session cassandra :test/session-1)]
  (alia/execute session :arche/insert-client {:values {:id "id-1" :name "Carol"}})
  (alia/execute session :arche/select-client {:values {:id "id-1"}})
  (alia/execute session :arche/insert-trade
    {:values {:id "trade-1"
              :asset-basket {"long" (arche/encode session
                                                  ::asset
                                                  {:code     "AB"
                                                   :currency "GBP"
                                                   :notional "12"})
                             "short" (arche/encode session
                                                   ::asset
                                                   {:code     "ZX"
                                                    :currency "AUD"
                                                    :notional "98"})}}}))
```

## TODO
Add tests

## License

Copyright © 2017 [Troy-West, Pty Ltd.](http://www.troywest.com)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
