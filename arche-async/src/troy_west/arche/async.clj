(ns troy-west.arche.async
  (:require [troy-west.arche :as arche]
            [qbits.alia.async :as alia.async]))

;; Note: execute-chan-buffered is really the only way of safely executing
;;       queries where returning a async/chan so we prefer execute vs. execute-chan-buffered
;;       as far as naming this particular function is concerned
(defn execute
  ([connection key]
   (arche/execute* alia.async/execute-chan-buffered connection key))
  ([connection key opts]
   (arche/execute* alia.async/execute-chan-buffered connection key opts)))

(defn execute-chan
  ([connection key]
   (arche/execute* alia.async/execute-chan connection key))
  ([connection key opts]
   (arche/execute* alia.async/execute-chan connection key opts)))