(ns less.sexy.utils)

(defn rand-str-generator [alphabet]
  (fn [n]
    (apply str (take n (repeatedly #(nth alphabet (.nextInt (java.util.Random.)
                                                            (count alphabet))))))))

(def cs "abcdefghijklnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")

(def rand-str (rand-str-generator cs))

