(ns troy-west.arche.async
  (:require [troy-west.arche :as arche]
            [qbits.alia.async :as async]))

(defn execute-chan
  ([connection query]
   (execute-chan connection query nil))
  ([connection query opts]
   (async/execute-chan (:session connection)
                       (or (arche/statement connection query) query)
                       opts)))

(defn execute-chan-buffered
  ([connection query]
   (execute-chan-buffered connection query nil))
  ([connection query opts]
   (async/execute-chan-buffered (:session connection)
                                (or (arche/statement connection query) query)
                                opts)))