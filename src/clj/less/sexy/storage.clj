(ns less.sexy.storage
  (:import java.util.concurrent.TimeUnit
           java.util.concurrent.Executors)
  (:require [ring.middleware.session.store :refer [SessionStore]]
            [less.sexy.utils :refer [rand-str log]]))

(defprotocol Storage
  (init! [this config] "Performs any side effects necessary to initialize the
                       storage medium. Returns a function that reverses these
                       side effects and closes the storage medium."))

(defprotocol IPhoneNumberStore
  (add! [this phone] "Adds a phone number to the store")
  (inc-auth! [this phone] "Increments the auth counter for a phone")
  (getphone [this number] "Gets the number record if it exists")
  (del! [this phone] "Removes a phone number from the store")
  (invalid! [this phone] "Completely deletes the phone record")
  (authorized-numbers [this] "Every active number in the store")
  (available-numbers [this] "Every unmatched active number in the store")
  (authorize! [this phone] "Authorizes the number")
  (pair-phone! [this p1 p2] "Pairs two phone numbers together")
  (pair-phones! [this] "Pairs every phone number in the system together"))

(defn- memory-blank-db []
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
          (catch java.io.IOException _
            (log "Database %s not found, using test data\n" path)
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
    {:pre [(map? phone)]}
    (swap! db assoc-in [:phones (:number phone)] phone)
    (getphone this (:number phone)))

  (inc-auth! [this phone]
    {:pre [(map? phone)]}
    (swap! db update-in [:phones (:number phone) :auth-attempts] inc)
    (getphone this (:number phone)))

  (authorize! [this phone]
    {:pre [(map? phone)]}
    (swap! db update-in [:phones (:number phone)] assoc :authorized true :valid true)
    (getphone this (:number phone)))

  (getphone [_ n]
    (get-in @db [:phones n]))

  (del! [_ phone]
    {:pre [(map? phone)]}
    (swap! db assoc-in [:phone (:number phone) :authorized] false)
    nil)

  (invalid! [_ phone]
    {:pre [(map? phone)]}
    (swap! db update-in [:phones (:number phone)] assoc :valid false :authorized false))

  (authorized-numbers [_]
    (->> (:phones @db)
      (map val)
      (filter :authorized)))

  (pair-phone! [this phone-1 phone-2]
    (swap! db assoc-in [:phones (:number phone-1) :partner] phone-2)
    (swap! db assoc-in [:phones (:number phone-2) :partner] phone-1))

  (pair-phones! [this]
    (let [available (shuffle (authorized-numbers this))
          pairs (partition-all 2 available)]
      (doseq [[p1 p2] pairs]
        (when p2 (pair-phone! this p1 p2)))))

  SessionStore
  (read-session [_ key]
    (get-in @db [:sessions key]))

  (write-session [_ key data]
    (let [key (or key (rand-str 30))]
      (swap! db assoc-in [:sessions key] data)
      key))

  (delete-session [_ key]
    (swap! db dissoc :sessions key)))


