(ns troy-west.arche.manifold
  (:require [troy-west.arche :as arche]
            [qbits.alia.manifold :as manifold]))

(defn execute
  ([connection query]
   (execute connection query nil))
  ([connection query opts]
   (manifold/execute (:session connection)
                     (or (arche/statement connection query) query)
                     opts)))

(defn execute-buffered
  ([connection query]
   (execute connection query nil))
  ([connection query opts]
   (manifold/execute-buffered (:session connection)
                              (or (arche/statement connection query) query)
                              opts)))