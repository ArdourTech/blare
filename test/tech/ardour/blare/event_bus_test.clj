(ns tech.ardour.blare.event-bus-test
  (:require
    [clojure.test :refer :all]
    [tech.ardour.blare.event-bus :as eb :refer [->PubSubEventBus]]
    [tech.ardour.blare.pub-sub :as ps]
    [tech.ardour.blare.pub-sub.sync :refer [->SyncPubSub]]))

(defmacro with-pub-sub-event-bus
  [event-bus-symb & body]
  `(let [ps# (doto (->SyncPubSub)
               (ps/start))
         ~event-bus-symb (->PubSubEventBus {:pub-sub ps#})]
     (try
       ~@body
       (finally
         (ps/shutdown ps#)))))

(deftest ->PubSubEventBus-test
  (let [[e1 e2 e-ignored] [:events/event1 :events/event2 :events/ignored]
        [data1 data2] (repeatedly gensym)]

    (testing "subscribe with a single keyword"
      (with-pub-sub-event-bus event-bus
        (let [captor (atom [])]
          (eb/subscribe event-bus e1 #(swap! captor conj (select-keys % [:type :data])))
          (eb/emit! event-bus [e-ignored data2])
          (eb/emit! event-bus [e1 data1])
          (is (= [{:type e1 :data data1}]
                 @captor))))

      (testing "subscribe with keyword set"
        (with-pub-sub-event-bus event-bus
          (let [captor (atom [])]
            (eb/subscribe event-bus #{e1 e2} #(swap! captor conj (select-keys % [:type :data])))
            (eb/emit! event-bus [e1 data1])
            (eb/emit! event-bus [e-ignored "ignored"])
            (eb/emit! event-bus [e2 data2])
            (is (= [{:type e1 :data data1}
                    {:type e2 :data data2}]
                   @captor))))))))
