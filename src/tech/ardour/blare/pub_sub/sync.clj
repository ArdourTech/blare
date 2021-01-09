(ns tech.ardour.blare.pub-sub.sync
  (:require
    [tech.ardour.blare.pub-sub.impl :as c :refer [->PubSub]]
    [tech.ardour.logging.core :as log]))

(defn ->SyncPubSub []
  (let [subscribers (atom {})]
    (log/info "Creating SyncPubSub")
    (->PubSub {:subscribers subscribers
               :publish     (fn sync-publish [topic message]
                              (c/notify-subscribers! (vals @subscribers) topic message))})))
