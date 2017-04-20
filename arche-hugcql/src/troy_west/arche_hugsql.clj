(ns troy-west.arche-hugcql
  (require [hugsql.core :as hugsql]))

(defn resolve-value
  [k]
  (str ":\"" (name (:name k)) "\""))

(defn resolve-identifier
  [k]
  (let [n (name (:name k))]
    (str (clojure.string/replace n #"-" "_") " as \"" n "\"")))

(defmulti resolve-key :type)

(defmethod resolve-key :default [k] k)
(defmethod resolve-key :v [k] (resolve-value k))
(defmethod resolve-key :value [k] (resolve-value k))
(defmethod resolve-key :i [k] (resolve-identifier k))
(defmethod resolve-key :identifier [k] (resolve-identifier k))

(defn resolve-keys
  [sql-template]
  (apply str (map resolve-key sql-template)))

(defn prepared-statements-from-pdefs
  [pdefs]
  (->> (for [{:keys [hdr sql] :as pdef} pdefs]
         [(keyword (first (:name hdr))) (resolve-keys sql)])
       (into {})))

(defn prepared-statements-from-string
  "Same as load-prepared-statements but takes a string as an input rather than a file."
  [s]
  (prepared-statements-from-pdefs
   (hugsql/parsed-defs-from-string s)))

(defn prepared-statements-from-map
  "Same as load-prepared-statements but takes a map as an input rather than a file.

  i.e.
    {:foo/bar \"select :i:foo-bar from emp as where id = :id\"}

  becomes

    {:foo/bar \"select foo_bar as \\\"foo-bar\\\" from emp as where id = :\\\"id\\\"\"}
  "
  [m]
  (->> (map identity m)
       flatten
       (map #(if (keyword? %)
               (str "--:name " (if (namespace %)
                                 (str (namespace %) "/" (name %))
                                 (name %)))
               %))
       (clojure.string/join "\n")
       prepared-statements-from-string))

(defn load-prepared-statements
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
  [& files]
  (apply merge
         (map (comp prepared-statements-from-pdefs
                    hugsql/parsed-defs-from-file)
              files)))

(defn prepared-statements
  "Convenience function that dispatches to the appropriate statement constructor

   arg - a map, String or a sequence of file paths."
  [arg]
  (cond
    (map? arg)        (prepared-statements-from-map arg)
    (string? arg)     (prepared-statements-from-string arg)
    (sequential? arg) (apply load-prepared-statements arg)))
