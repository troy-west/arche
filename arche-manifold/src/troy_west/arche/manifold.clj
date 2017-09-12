(ns troy-west.arche.manifold
  (:require [troy-west.arche :as arche]
            [qbits.alia.manifold :as manifold]))

(defn execute
  ([connection key]
   (arche/execute* manifold/execute connection key))
  ([connection key opts]
   (arche/execute* manifold/execute connection key opts)))

(defn execute-buffered
  ([connection key]
   (arche/execute* manifold/execute-buffered connection key))
  ([connection key opts]
   (arche/execute* manifold/execute-buffered connection key opts)))