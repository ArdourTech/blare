(ns tech.ardour.blare.event-bus
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [tech.ardour.blare.pub-sub :as ps]
    [tech.ardour.blare.util :as u]))

(defprotocol EventBus
  (-emit [this event])
  (subscribe [this selector callback]))

(defrecord Event [type id created-at data])

(def EventBus? (partial satisfies? EventBus))
(def Event? (partial instance? Event))

(defn ->Event [type data]
  (map->Event {:type    type
               :data    data
               :id      (u/random-uuid)
               :created (u/epoch-milli)}))

(defn- vectorify-event [event-or-events]
  (condp #(%1 %2) (first event-or-events)
    keyword?
    [event-or-events]

    vector?
    event-or-events

    (throw (ex-info "Invalid Event" {:event event-or-events}))))

(defn emit!
  [bus event-or-events]
  {:pre [(EventBus? bus)]}
  (doseq [[event-type event-data] (vectorify-event event-or-events)]
    (->> (->Event event-type event-data)
         (-emit bus))))

(defn- topical-event [events-ns event-type-k]
  {:pre [(keyword? event-type-k)]}
  (str events-ns (.-sym event-type-k)))

(defrecord PubSubEventBus [pub-sub events-ns]
  EventBus

  (-emit [_ {:keys [type] :as event}]
    (ps/publish pub-sub (topical-event events-ns type) event))

  (subscribe [_ selector callback]
    (if (keyword? selector)
      (ps/subscribe pub-sub
        (topical-event events-ns selector)
        {:on-message callback})
      (ps/subscribe pub-sub
        (str events-ns "*")
        {:on-message (fn [{:keys [type] :as event}]
                       (when (selector type)
                         (callback event)))}))))

(defn ->PubSubEventBus [{:keys [pub-sub events-ns]}]
  {:pre [(ps/PubSub? pub-sub)]}
  (let [events-ns (or events-ns (gensym "events"))
        events-ns (if (str/ends-with? events-ns ".")
                    events-ns
                    (str events-ns "."))]
    (PubSubEventBus. pub-sub events-ns)))

(s/def ::event-bus EventBus?)
(s/def ::event Event?)

(s/fdef emit!
  :args (s/cat
          :bus ::event-bus
          :event (s/spec (s/or :single ::event
                               :multiple (s/coll-of ::event)))))

(s/def ::event-bus EventBus?)
(s/def ::event (s/cat :type keyword? :data any?))

(s/fdef subscribe
  :args (s/cat
          :bus ::event-bus
          :selector (s/spec (s/or :keyword keyword?
                                  :keywords (s/coll-of keyword? :kind set)
                                  :function fn?))))

(s/fdef ->Event
  :args (s/cat
          :type string?
          :data any?))

