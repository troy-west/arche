(ns troy-west.arche.manifold
  (:require [troy-west.arche :as arche]
            [qbits.alia.manifold :as manifold]))

(defn execute
  ([connection query]
   (arche/execute* manifold/execute connection query))
  ([connection query opts]
   (arche/execute* manifold/execute connection query opts)))

(defn execute
  ([connection query]
   (arche/execute* manifold/execute-buffered connection query))
  ([connection query opts]
   (arche/execute* manifold/execute-buffered connection query opts)))