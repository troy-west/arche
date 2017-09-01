(ns troy-west.arche.async
  (:require [troy-west.arche :as arche]
            [qbits.alia.async :as alia.async]))

(defn execute-chan
  ([connection query]
   (arche/execute* alia.async/execute-chan connection query))
  ([connection query opts]
   (arche/execute* alia.async/execute-chan connection query opts)))

(defn execute-chan-buffered
  ([connection query]
   (arche/execute* alia.async/execute-chan-buffered connection query))
  ([connection query opts]
   (arche/execute* alia.async/execute-chan-buffered connection query opts)))