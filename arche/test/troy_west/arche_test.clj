(ns troy-west.arche-test
  (:require [clojure.test :refer [deftest is]]
            [troy-west.arche :as arche]
            [qbits.alia :as alia]
            [qbits.alia.udt :as alia.udt]))

(deftest prepare-statements

  (is (= {} (arche/prepare-statements nil nil)))

  (is (= {:query-1 {:cql      "a-query"
                    :prepared "prepared: a-query"}
          :query-2 {:cql      "b-query"
                    :prepared "prepared: b-query"}
          :query-3 {:cql      "c-query"
                    :prepared "prepared: c-query"
                    :opts     {:fetch-size 5000}}}
         (with-redefs [alia/prepare (fn [_ cql] (format "prepared: %s" cql))]
           (arche/prepare-statements nil
                                     {:query-1 "a-query"
                                      :query-2 {:cql "b-query"}
                                      :query-3 {:cql  "c-query"
                                                :opts {:fetch-size 5000}}})))))

(deftest prepare-encoders

  (is (= {} (arche/prepare-encoders nil nil)))

  (is (= {:udt-1 {:prepared true}
          :udt-2 {:prepared true}}
         (with-redefs [alia.udt/encoder (fn [_ name codec] {:prepared true})]
           (arche/prepare-encoders nil
                                   {:udt-1 {}
                                    :udt-2 {}})))))

(deftest options

  (is (= nil
         (arche/options {} ::select nil)))

  (is (= {:fetch-size 10}
         (arche/options {} ::select {:fetch-size 10})))

  (is (= {:fetch-size 10}
         (arche/options {:statements {::select {:opts {:fetch-size 20}}}} ::select {:fetch-size 10})))

  (is (= {:fetch-size 20}
         (arche/options {:statements {::select {:opts {:fetch-size 20}}}} ::select nil)))

  (is (= {:fetch-size 20
          :channel    "chan"}
         (arche/options {:statements {::select {:opts {:fetch-size 20}}}} ::select {:channel "chan"}))))

(deftest execute*

  (let [statement (atom nil)
        options   (atom nil)
        f         (fn capture
                    ([_ statement*]
                     (capture _ statement* nil))
                    ([_ statement* options*]
                     (reset! statement statement*)
                     (reset! options options*)))]

    (is (= [nil nil]
           (do (arche/execute* f
                               {}
                               ::select)
               [@statement @options])))

    (is (= ["prepared" nil]
           (do (arche/execute* f
                               {:statements {::select {:prepared "prepared"}}}
                               ::select)
               [@statement @options])))

    (is (= ["prepared" {:fetch-size 10}]
           (do (arche/execute* f {:statements {::select {:prepared "prepared"}}}
                               ::select
                               {:fetch-size 10})
               [@statement @options])))

    (is (= ["prepared" {:fetch-size 10}]
           (do (arche/execute* f {:statements {::select {:prepared "prepared"
                                                         :opts     {:fetch-size 5000}}}}
                               ::select
                               {:fetch-size 10})
               [@statement @options])))

    (is (= ["prepared" {:fetch-size 5000}]
           (do (arche/execute* f {:statements {::select {:prepared "prepared"
                                                         :opts     {:fetch-size 5000}}}}
                               ::select)
               [@statement @options])))

    (is (= ["prepared" {:fetch-size 5000}]
           (do (arche/execute* f {:statements {::select {:prepared "prepared"
                                                         :opts     {:fetch-size 5000}}}}
                               ::select
                               nil)
               [@statement @options])))

    (is (= ["prepared" {:fetch-size 5000
                        :channel    "chan"}]
           (do (arche/execute* f {:statements {::select {:prepared "prepared"
                                                         :opts     {:fetch-size 5000}}}}
                               ::select
                               {:channel "chan"})
               [@statement @options])))))