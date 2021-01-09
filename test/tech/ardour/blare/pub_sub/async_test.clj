(ns tech.ardour.blare.pub-sub.async-test
  (:require
    [clojure.test :refer :all]
    [tech.ardour.blare.pub-sub :as ps]
    [tech.ardour.blare.pub-sub.async :refer [->AsyncPubSub]]
    [tech.ardour.blare.pub-sub.test-util :refer :all]))

(defmacro with-async-pub-sub
  [pub-sub-symb & body]
  `(let [~pub-sub-symb (doto (->AsyncPubSub)
                         (ps/start))]
     (try
       ~@body
       (finally
         (ps/shutdown ~pub-sub-symb)))))

(deftest ->AsyncPubSub-test

  (with-async-pub-sub pub-sub
    (assert-one-to-one-subscriber pub-sub))

  (with-async-pub-sub pub-sub
    (assert-one-to-many-subscriber pub-sub))

  (with-async-pub-sub pub-sub
    (assert-specified-subscription-id-subscriber pub-sub))

  (with-async-pub-sub pub-sub
    (assert-basic-wildcard-subscriptions pub-sub)))

