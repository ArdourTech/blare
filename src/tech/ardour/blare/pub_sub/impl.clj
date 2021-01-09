(ns tech.ardour.blare.pub-sub.impl
  (:require
    [clojure.string :as str]
    [tech.ardour.blare.event-bus :refer [EventBus]]
    [tech.ardour.blare.pub-sub :as ps :refer [PubSub]]
    [tech.ardour.logging.core :as log])
  (:import
    (java.util UUID)))

(defn ->PubSub [{:keys [publish start shutdown subscribers] :as fns}]
  (reify PubSub
    (publish [_ topic message]
      (log/debug "Publish" {:topic   topic
                            :message message})
      (publish topic message))

    (subscribe [_ topic-pattern {:keys [on-message subscription-id] :as opts}]
      (when (and subscription-id
                 (get @subscribers subscription-id))
        (log/warn "Overriding Subscription" {:topic-pattern   topic-pattern
                                             :subscription-id subscription-id}))
      (let [subscription-id (or subscription-id (UUID/randomUUID))]
        (log/debug "Subscribing" {:topic-pattern   topic-pattern
                                  :subscription-id subscription-id})
        (swap! subscribers assoc subscription-id [topic-pattern on-message])
        subscription-id))

    (unsubscribe [_ subscription-id]
      (log/debug "Unsubscribing" {:subscription-id subscription-id})
      (if (get @subscribers subscribers)
        (do
          (swap! subscribers dissoc subscription-id)
          :ok)
        :unknown))

    (subscribers [_]
      @subscribers)

    (start [_]
      (when start
        (start)))

    (shutdown [_]
      (when shutdown
        (shutdown)))))

(defn- topic-match? [pattern topic]
  (if (str/includes? pattern "*")
    (str/starts-with? topic (str/replace pattern "*" ""))
    (= pattern topic)))

(defn notify-subscribers! [subscriptions topic message]
  (let [processed? (volatile! false)
        ctx {:message message
             :topic   topic}]
    (doseq [[topic-pattern callback] subscriptions
            :when (topic-match? topic-pattern topic)]
      (try
        (callback message)
        (vreset! processed? true)
        (catch Exception e
          (log/error e "Error processing message" (assoc ctx :topic-pattern topic-pattern)))))
    (when-not @processed?
      (log/warn "Unprocessed message" ctx))))
