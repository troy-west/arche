(ns troy-west.arche.manifold
  (:require [troy-west.arche :as arche]
            [qbits.alia.manifold :as manifold]))

(defn execute
  ([connection executable]
   (arche/execute* manifold/execute connection executable))
  ([connection executable opts]
   (arche/execute* manifold/execute connection executable opts)))

(defn execute-buffered
  ([connection executable]
   (arche/execute* manifold/execute-buffered connection executable))
  ([connection executable opts]
   (arche/execute* manifold/execute-buffered connection executable opts)))