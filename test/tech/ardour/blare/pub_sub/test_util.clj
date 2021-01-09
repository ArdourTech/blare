(ns tech.ardour.blare.pub-sub.test-util
  (:require
    [clojure.test :refer :all]
    [tech.ardour.blare.pub-sub :as ps]
    [tech.ardour.blare.util :as u]))

(def default-death-ms (* 5 1000))
(def default-wait-ms 10)

(defn wait-until*
  "wait until a function has become true"
  ([name f] (wait-until* name f default-death-ms))
  ([name f wait-death]
   {:pre [(some? name)
          (fn? f)
          (pos? wait-death)]}
   (let [die (+ (u/epoch-milli) wait-death)]
     (loop []
       (if-let [result (f)]
         result
         (do
           (Thread/sleep default-wait-ms)
           (if (<= die (u/epoch-milli))
             (throw (ex-info "Timeout waiting for result" {:name name}))
             (recur))))))))

(defmacro wait-until
  [expr]
  `(wait-until*
     ~(pr-str expr)
     (fn [] ~expr)))

(defn assert-one-to-one-subscriber [pub-sub]
  (testing "one to one subscriber"
    (let [captor (atom [])
          f #(swap! captor conj %)
          topic "topic1"
          message1 "message1"
          message2 "message2"
          subscriber-id (ps/subscribe pub-sub topic {:on-message f})]
      (is (uuid? subscriber-id))
      (is (= {subscriber-id [topic f]}
             (ps/subscribers pub-sub)))
      (doseq [[topic message] [[topic message1]
                               [(str "not." topic) "ignored"]
                               [topic message2]]]
        (ps/publish pub-sub topic message))
      (is (wait-until (= [message1 message2]
                         @captor))))))

(defn assert-one-to-many-subscriber [pub-sub]
  (testing "one to many subscriber"
    (let [captor (atom [])
          f1 #(swap! captor conj {:subscriber 1 :message %})
          f2 #(swap! captor conj {:subscriber 2 :message %})
          topic "topic"
          message1 "message1"
          message2 "message2"
          subscriber-1-id (ps/subscribe pub-sub topic {:on-message f1})
          subscriber-2-id (ps/subscribe pub-sub topic {:on-message f2})]
      (is (every? uuid? [subscriber-1-id subscriber-2-id]))
      (is (= {subscriber-1-id [topic f1]
              subscriber-2-id [topic f2]}
             (ps/subscribers pub-sub)))
      (doseq [[topic message] [[topic message1]
                               [(str "not." topic) "ignored"]
                               [topic message2]]]
        (ps/publish pub-sub topic message))
      (is (wait-until (every? #{{:subscriber 1 :message message1}
                                {:subscriber 2 :message message1}
                                {:subscriber 1 :message message2}
                                {:subscriber 2 :message message2}}
                        @captor))))))

(defn assert-specified-subscription-id-subscriber [pub-sub]
  (testing "allows a specified subscription identifier"
    (let [captor (atom [])
          on-message #(swap! captor conj [%1 %2])
          topic "topic"
          message1 "message1"
          message2 "message1"
          subscription-id ::subscription-id
          subscriber-1-id (ps/subscribe pub-sub topic {:subscription-id subscription-id
                                                       :on-message      (partial on-message "first")})]
      (is (= subscription-id subscriber-1-id))
      (ps/publish pub-sub topic message1)
      (is (wait-until (= [["first" message1]] @captor)))

      (testing "re-subscribing overrides the subscription"
        (ps/subscribe pub-sub topic {:subscription-id subscription-id
                                     :on-message      (partial on-message "second")})
        (ps/publish pub-sub topic message1)
        (is (wait-until (= [["first" message1]
                            ["second" message2]]
                           @captor)))))))

(defn assert-basic-wildcard-subscriptions [pub-sub]
  (testing "a subscriber can receive topics using a basic wildcard (*)"
    (let [captor (atom [])
          message1 "message1"
          message2 "message2"
          topic "topic."
          topic1 (str topic "match1")
          topic2 (str topic "match2")
          on-message #(swap! captor conj %)]
      (ps/subscribe pub-sub (str topic "*") {:on-message on-message})
      (ps/publish pub-sub topic1 message1)
      (ps/publish pub-sub "ignored" "ignored")
      (ps/publish pub-sub topic2 message2)
      (is (wait-until (= [message1 message2]
                         @captor))))))
