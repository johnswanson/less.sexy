(ns less.sexy.storage
  (:import java.util.concurrent.TimeUnit
           java.util.concurrent.Executors)
  (:require [ring.middleware.session.store :refer [SessionStore]]))

(def cs (map char (concat (range 48 58) (range 66 92) (range 97 123))))
(defn rand-char [] (nth cs (.nextInt (java.util.Random.) (count cs))))
(defn rand-str [n] (apply str (take n (repeatedly rand-char))))

(defprotocol Storage
  (init! [this config] "Performs any side effects necessary to initialize the
                       storage medium. Returns a function that reverses these
                       side effects and closes the storage medium."))

(defprotocol IPhoneNumberStore
  (add-number! [this number] "Adds a phone number to the store")
  (del-number! [this number] "Removes a phone number from the store")
  (active-numbers [this] "Every active number in the store"))

(defn memory-blank-db []
  {:sessions {}
   :numbers {}})

(defrecord MemoryStorage [db]
  Storage
  (init! [_ {:keys [path to-disk?]}]
    (let [persist-db (fn [] (when to-disk? (spit path (str @db))))
          read-db (fn [] (read-string (slurp path)))]
      (if to-disk?
        (try
          (reset! db (read-db))
          (catch java.io.IOException ioe
            (println "Database" path "not found, using test data")
            (reset! db (memory-blank-db))))
        (reset! db (memory-blank-db)))
      (let [shutdown-thread (Thread. persist-db)
            shutdown-hook (..
                            Runtime
                            getRuntime
                            (addShutdownHook shutdown-thread))
            thread-pool (Executors/newScheduledThreadPool 1)
            scheduled-exec (.
                            thread-pool
                            (scheduleAtFixedRate persist-db
                                                 (long 1)
                                                 (long 1)
                                                 (. TimeUnit MINUTES)))]
        #(do (.shutdown thread-pool)
             (.. Runtime getRuntime (removeShutdownHook shutdown-thread))
             (persist-db)))))

  IPhoneNumberStore
  (add-number! [_ number]
    (let [num-obj {:active true
                   :number number}]
      (swap! db assoc-in [:numbers number] num-obj)))

  (del-number! [_ number]
    (swap! db assoc-in [:numbers number :active] false))

  (active-numbers [_]
    (->> (:numbers @db)
      (map val)
      (filter :active)
      (map :number)))

  SessionStore
  (read-session [_ key]
    (get-in @db [:sessions key]))

  (write-session [_ key data]
    (let [key (or key (rand-str 30))]
      (swap! db assoc-in [:sessions key] data)
      key))

  (delete-session [_ key]
    (swap! db dissoc :sessions key)))


