(ns troy-west.arche.hugcql
  (:refer-clojure :exclude [load])
  (:require [hugsql.core :as hugsql]))

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

(defn statement-cql
  [sql-keys]
  (apply str (map resolve-key sql-keys)))

(defn statements
  [pdefs]
  (->> (for [{:keys [hdr sql] :as pdef} pdefs]
         [(statement-key hdr) (statement-cql sql)])
       (into {})))

(defn parse
  "Same as load-prepared-statements but takes a string as an input rather than a file."
  [text]
  (statements (hugsql/parsed-defs-from-string text)))

(defn load
  "Reads named CQL statements (using Hugsql formatting) and returns
   a map of key (statement name) -> statement string.

   The statement strings will have any Hugsql Values and Identifiers
   quoted.

   i.e.
     --:name foo/bar
     select :i:foo-bar from emp where id = :id

   becomes

     {:foo/bar \"select foo_bar as \\\"foo-bar\\\" from emp where id = :\\\"id\\\"\"}

  See: https://www.hugsql.org/
       https://www.hugsql.org/#param-value
       https://www.hugsql.org/#param-identifier

  Note: The other more dynamic features of Hugsql are not supported here,
        list, tuples, snippets.
  "
  [path]
  (statements (hugsql/parsed-defs-from-file path)))
