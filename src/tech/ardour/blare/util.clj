(ns tech.ardour.blare.util
  (:import
    (java.util UUID)
    (java.time Instant)))

(defn random-uuid []
  (UUID/randomUUID))

(defn epoch-milli []
  (-> (Instant/now)
      (.toEpochMilli)))
