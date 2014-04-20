(ns less.sexy.system
  (:gen-class)
  (:require [less.sexy.storage :as storage]
            [less.sexy.twilio :as twilio]
            [less.sexy.routing]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import [less.sexy.storage MemoryStorage]
           [less.sexy.twilio Twilio]))

(defn config [& args]
  (let [data (read-string (slurp "config.clj"))]
    (get-in data args)))

(defn system []
  {:storage nil
   :twilio nil
   :server nil})

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
        server (run-jetty
                 (less.sexy.routing/create-handler store twilio)
                 (config :server))]
    (-> system
      (assoc :storage {:storage store :shutdown shutdown-store})
      (assoc :twilio twilio)
      (assoc :server server))))

(defn stop!
  "Performs side effects to shut down the system and release its resources.
  Returns an updated instance of the system."
  [system]
  (.stop (:server system))
  ((get-in system [:storage :shutdown]))
  (-> system
    (assoc :server nil)
    (assoc :storage nil)
    (assoc :twilio nil)))

(defn -main [] (start! (system)))

