(ns less.sexy.utils)

(def log printf)

(defn rand-str-generator [alphabet]
  (fn [n]
    (apply str (take n (repeatedly #(nth alphabet (.nextInt (java.util.Random.)
                                                            (count alphabet))))))))

(def ^:private cs "abcdefghijklnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")

(def rand-str (rand-str-generator cs))

