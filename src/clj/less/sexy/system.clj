(ns less.sexy.system
  (:gen-class)
  (:require [clojure.core.async :as async]
            [less.sexy.storage :as storage]
            [less.sexy.twilio :as twilio]
            [less.sexy.routing]
            [less.sexy.numbers]
            [ring.adapter.jetty :refer [run-jetty]]
            [org.httpkit.server :refer [run-server]])
  (:import [less.sexy.storage MemoryStorage]
           [less.sexy.twilio Twilio]
           [less.sexy.numbers PhoneNumbers]))

(defn config [& args]
  (let [data (read-string (slurp "config.clj"))]
    (get-in data args)))

(defn system []
  {:storage nil
   :twilio nil
   :server nil
   :session nil
   :channels nil})

(defn start!
  "Performs side effects to initialize the system, aquire resources, and start
  it running. Returns an updated instance of the system."
  [system]
  (let [store (case (config :storage :type)
                :memory (new MemoryStorage (atom nil))
                nil)
        shutdown-store (storage/init! store (config :storage))
        twilio (new Twilio
                    (config :twilio :sid)
                    (config :twilio :token)
                    (config :twilio :number))
        channels (atom {})
        session {:store store}
        numbers (new PhoneNumbers store twilio channels)
        server (run-server
                 (less.sexy.routing/create-handler session numbers channels twilio)
                 (config :server))]
    (-> system
      (assoc :storage {:storage store :shutdown shutdown-store})
      (assoc :twilio twilio)
      (assoc :server server)
      (assoc :session session)
      (assoc :channels channels))))

(defn stop!
  "Performs side effects to shut down the system and release its resources.
  Returns an updated instance of the system."
  [system]
  ((:server system) :timeout 100)
  ((get-in system [:storage :shutdown]))
  (-> system
    (assoc :server nil)
    (assoc :storage nil)
    (assoc :twilio nil)
    (assoc :session nil)
    (assoc :channels nil)))

(defn -main [] (start! (system)))

