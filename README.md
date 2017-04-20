# Arche: Cassandra Clojure Cluster and Session Management with Alia

> [Arche](https://en.wikipedia.org/wiki/Arche): A Greek word with primary senses "beginning", "origin", or "source of action"<br/>
> [Arche](https://en.wikipedia.org/wiki/Arche_(mythology)): The ancient Greek muse of origins

## Modules

* [com.troy-west.arche/arche](https://github.com/troy-west/arche/tree/master/arche)

  The base arche module. Provides an alia session wrapper that binds prepared statements to a session allowing them to be called using the standard alia execution functions. UDT's are also bound to the session.

* [com.troy-west.arche/arche-hugsql](https://github.com/troy-west/arche/tree/master/arche-hugsql)

  Uses hugsql to parse prepared statements from a String, File or map of statements.

* [com.troy-west.arche/arche-integrant](https://github.com/troy-west/arche/tree/master/arche-integrant)

  Provides an opinionated cassandra component (using [integrant](https://github.com/weavejester/integrant)) with lifecycle and dependency management of clusters and sessions.

* [com.troy-west.arche/arche-component](https://github.com/troy-west/arche/tree/master/arche-component)

  Provides an opinionated cassandra component (using [Component](https://github.com/stuartsierra/component)) with lifecycle and dependency management of clusters and sessions.

## Usage

### Initial Setup

Define your schema (and automate with [CCM-CLJ](https://github.com/SMX-LTD/ccm-clj))

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

Example of creating a cluster:

``` clojure
(require '[ccm-clj.core :as ccm])
(ccm/auto-cluster! "arche"
                   "2.2.6"
                   3
                   [#"test-resources/test-keyspace\.cql"]
                   {"sandbox" [#"test-resources/test-tables\.cql"]}
                   19142)
```

### Usage without components

Create a cluster instance using alia.

```clojure
(require '[qbits.alia :as alia])

(def cluster (alia/cluster {:contact-points ["127.0.0.1"] :port 19142}))
```

Define some statements and UDT's.

``` clojure

;; note: the keys here define data reader tag names
(def udts {::asset {:name "asset"}})

(def statements {::insert-client "INSERT INTO client (id, name) VALUES (:id, :name)"
                 ::select-client "SELECT * FROM client WHERE id = :id"
                 ::insert-trade  "INSERT INTO trade (id, asset_basket) VALUES (:id, :\"asset-basket\")"
                 ::select-trade  "SELECT id, asset_basket as \"asset-basket\" FROM trade where id = :id"})
```

Create an arche session with the cluster and the sandbox keyspace and pass the statements and UDT's as options.

``` clojure
(require '[com.troy-west.arche :as arche])

(def session (arche/connect cluster "sandbox" {:statements statements :udts udts}))
```

Use new UDT encoders.

``` clojure
(arche/encode session ::asset {:code     "AB"
                               :currency "GBP"
                               :notional "12"}

;; creates UDTValue instance
;; #object[com.datastax.driver.core.UDTValue 0x29632e50 "{code:'AB',currency:'GBP',notional:'12'}"]
```

Use alia execute queries against the session.

``` clojure
(alia/execute session ::insert-client {:values {:id "id-1" :name "Carol"}})
```

### Parsing CQL prepared statements

Arche-hugsql makes use of [HugSQL](https://www.hugsql.org/) to parse CQL prepared statements defined in CQL files, Strings or in a clojure map. It supports HugSQL [value parameters](https://www.hugsql.org/#param-value) and [identifier parameters](https://www.hugsql.org/#param-identifier). You can include the keywords in your CQL queries and the names will be properly quoted for you, automatically handling kebab cased keywords.

The clq parsing is all done through the `prepared-statements` function. This function produces a map of prepared statement Strings with the identifiers and values quoted.

#### maps example

``` clojure
(require '[com.troy-west.arche-hugsql :as arche-hugsql])

(arche-hugsql/prepared-statements {:foo/bar "select :i:foo-bar from emp as where id_num = :id-num"})
;; => {:foo/bar "select foo_bar as \"foo-bar\" from emp as where id_num = :\"id-num\""}
```

#### Strings example

``` clojure
(arche-hugsql/prepared-statements "--:name foo/bar \nselect :i:foo-bar from emp where id_num = :id-num")
;; => {:foo/bar "select foo_bar as \"foo-bar\" from emp as where id_num = :\"id-num\""}
```

#### Files example

Create a file in the resources directory called foo_bar.cql

``` text
--:name foo/bar
select :i:foo-bar from emp where id = :id
```

Then cql file can be loaded like this:

``` clojure
(arche-hugsql/prepared-statements ["foo_bar.cql"])
;; => {:foo/bar "select foo_bar as \"foo-bar\" from emp as where id_num = :\"id-num\""}
```

### [integrant](https://github.com/weavejester/integrant) arche component (recommended)

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
(require '[com.troy-west.arche-integrant :as arche])

(def cassandra-config
  {[:arche/statements :test/statements-1] ["prepared/test.cql"]
   [:arche/udts :test/udts-1]             {::asset {:name "asset"}}
   [:cassandra/cluster :test/cluster-1]   {:contact-points ["127.0.0.1"] :port 19142}
   [:cassandra/session :test/session-1]   {:keyspace   "sandbox"
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

### [Component](https://github.com/stuartsierra/component) arche component

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

Note: the data reader tags #arche/statements, #arche/cluster and #arche/session are made avalible by the `arche-component` library to make it possible to declaratively define the Component configuration as EDN data.

``` clojure
(require '[com.stuartsierra.component :as component])
(require '[com.troy-west.arche-component :as arche])

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
(def cassandra (component/start (component/map->SystemMap cassandra-config))
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
