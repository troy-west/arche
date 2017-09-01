(ns troy-west.arche.async
  (:require [troy-west.arche :as arche]
            [qbits.alia.async :as alia.async]))

(defn execute-chan
  ([connection key]
   (arche/execute* alia.async/execute-chan connection key))
  ([connection key opts]
   (arche/execute* alia.async/execute-chan connection key opts)))

(defn execute-chan-buffered
  ([connection key]
   (arche/execute* alia.async/execute-chan-buffered connection key))
  ([connection key opts]
   (arche/execute* alia.async/execute-chan-buffered connection key opts)))