(ns less.sexy.storage
  (:import java.util.concurrent.TimeUnit
           java.util.concurrent.Executors)
  (:require [ring.middleware.session.store :refer [SessionStore]]
            [less.sexy.utils :refer [rand-str]]))

(defprotocol Storage
  (init! [this config] "Performs any side effects necessary to initialize the
                       storage medium. Returns a function that reverses these
                       side effects and closes the storage medium."))

(defprotocol IPhoneNumberStore
  (add! [this phone] "Adds a phone number to the store")
  (inc-auth! [this phone] "Increments the auth counter for a phone")
  (getphone [this number] "Gets the number record if it exists")
  (del! [this phone] "Removes a phone number from the store")
  (authorized-numbers [this] "Every active number in the store")
  (authorize! [this phone] "Authorizes the number"))

(defn memory-blank-db []
  {:sessions {}
   :phones {}})

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
  (add! [this phone]
    (swap! db assoc-in [:phones (:number phone)] phone)
    (getphone this (:number phone)))

  (inc-auth! [this phone]
    (swap! db update-in [:phones (:number phone) :auth-attempts] inc)
    (getphone this (:number phone)))

  (authorize! [this phone]
    (swap! db assoc-in [:phones (:number phone) :authorized] true)
    (getphone this (:number phone)))

  (getphone [_ n]
    (get-in @db [:phones n]))

  (del! [_ phone]
    (assert (map? phone))
    (swap! db assoc-in [:phone (:number phone) :authorized] false)
    nil)

  (authorized-numbers [_]
    (->> (:phones @db)
      (map val)
      (filter :authorized)
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


