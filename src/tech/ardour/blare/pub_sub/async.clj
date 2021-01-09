(ns tech.ardour.blare.pub-sub.async
  (:require
    [clojure.core.async :as async]
    [tech.ardour.blare.pub-sub.impl :as c :refer [->PubSub]]
    [tech.ardour.logging.core :as log]))

(def one-mb 1048576)

(defn ->AsyncPubSub [& [buffer-size]]
  (let [buffer-size (or buffer-size one-mb)
        in-chan (async/chan (async/sliding-buffer one-mb))
        subscribers (atom {})]
    (log/info :msg "Creating AsyncPubSub" :buffer-size buffer-size)
    (->PubSub {:subscribers subscribers
               :start       (fn -async-pub-sub-start []
                              (async/go-loop []
                                (when-some [[topic message] (async/<! in-chan)]
                                  (c/notify-subscribers! (vals @subscribers) topic message)
                                  (recur))))
               :shutdown    (fn -async-pub-sub-stop []
                              (log/info :msg "Stopping AsyncPubSub")
                              (async/close! in-chan))
               :publish     (fn async-publish [topic message]
                              (async/put! in-chan [topic message]))})))
