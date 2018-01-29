(ns troy-west.arche.hugcql
  (:refer-clojure :exclude [load])
  (:require [hugsql.core :as hugsql]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(defn resolve-value
  [key]
  (let [alias (name (:name key))]
    (if (.contains alias "-")
      (format ":\"%s\"" alias)
      (format ":%s" alias))))

(defn resolve-identifier
  [key]
  (let [alias (name (:name key))]
    (if (.contains alias "-")
      (let [column (clojure.string/replace alias #"-" "_")]
        (format "%s as \"%s\"" column alias))
      alias)))

(defmulti resolve-key :type)

(defmethod resolve-key :default [k] k)
(defmethod resolve-key :v [k] (resolve-value k))
(defmethod resolve-key :value [k] (resolve-value k))
(defmethod resolve-key :i [k] (resolve-identifier k))
(defmethod resolve-key :identifier [k] (resolve-identifier k))

(defn statement-key
  [hdr]
  (keyword (first (:name hdr))))

(defn statement-options
  [hdr]
  (when-let [opts-parts (:options hdr)]
    (try
      ;; TODO: someone remind me why I can't just do this?
      (edn/read-string (str/join " " opts-parts))
      ;; naughty!
      (catch Exception _))))

(defn statement-cql
  [sql-keys]
  (str/trim (str/join (map resolve-key sql-keys))))

(defn statements
  [pdefs]
  (into {}
        (for [{:keys [hdr sql]} pdefs]
          [(statement-key hdr) (if-let [opts (statement-options hdr)]
                                 {:cql  (statement-cql sql)
                                  :opts opts}
                                 (statement-cql sql))])))

(defn parse
  "Same as load-prepared-statements but takes a string as an input rather than a file."
  [text]
  (statements (hugsql/parsed-defs-from-string text)))

(defn load
  "Reads named HugCQL files (using HugSQL formatting) and returns
   a map of key (statement name) -> statement string / opts if defined.

   Statements will be have values and identifiers automatically hyphen/underscore translated.

   i.e.
     --:name foo/bar
     select :i:foo-bar from emp where id = :id

   becomes

     {:foo/bar \"select foo_bar as \\\"foo-bar\\\" from emp where id = :id\"}

   Options can be provided in EDN format that are defaulted on statement execution

   i.e.
     --:name foo/bar
     --:options {:fetch-size 500}
     select :i:foo-bar from emp where id = :id

   becomes

     {:foo/bar {:cql  \"select foo_bar as \\\"foo-bar\\\" from emp where id = :id\"
                :opts {:fetch-size 500}}

  See: https://www.hugsql.org/
       https://www.hugsql.org/#param-value
       https://www.hugsql.org/#param-identifier

  Note: The other more dynamic features of Hugsql are not supported here, list, tuples, snippets.
  "
  [path]
  (statements (hugsql/parsed-defs-from-file path)))
