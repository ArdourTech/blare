(ns tech.ardour.blare.pub-sub
  (:require
    [clojure.spec.alpha :as s]))

(defprotocol PubSub
  (publish [this topic message])
  (subscribe [this topic-pattern opts])
  (unsubscribe [this subscription])
  (subscribers [this])
  (start [this])
  (shutdown [this]))

(def PubSub? (partial satisfies? PubSub))
(s/def ::pub-sub PubSub?)

(s/fdef publish
  :args (s/cat
          :pubsub ::pub-sub
          :topic string?
          :message any?))
