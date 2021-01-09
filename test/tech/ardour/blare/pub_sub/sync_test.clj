(ns tech.ardour.blare.pub-sub.sync-test
  (:require
    [clojure.test :refer :all]
    [tech.ardour.blare.pub-sub :as ps]
    [tech.ardour.blare.pub-sub.sync :refer [->SyncPubSub]]
    [tech.ardour.blare.pub-sub.test-util :refer :all]))

(defmacro with-sync-pub-sub
  [pub-sub-symb & body]
  `(let [~pub-sub-symb (doto (->SyncPubSub)
                         (ps/start))]
     (try
       ~@body
       (finally
         (ps/shutdown ~pub-sub-symb)))))

(deftest ->SyncPubSub-test

  (with-sync-pub-sub pub-sub
    (assert-one-to-one-subscriber pub-sub))

  (with-sync-pub-sub pub-sub
    (assert-one-to-many-subscriber pub-sub))

  (with-sync-pub-sub pub-sub
    (assert-specified-subscription-id-subscriber pub-sub))

  (with-sync-pub-sub pub-sub
    (assert-basic-wildcard-subscriptions pub-sub)))
