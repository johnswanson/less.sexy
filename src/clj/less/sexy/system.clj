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
  (let [storage (atom nil)
        handler nil]
    {:storage {:store storage}
     :twilio nil
     :server {:handler handler
              :server nil}}))

(defn start!
  "Performs side effects to initialize the system, aquire resources, and start
  it running. Returns an updated instance of the system."
  [system]
  (let [store (case (config :storage :type)
                :memory (new MemoryStorage (atom nil))
                nil)
        twilio (new Twilio
                    (config :twilio :sid)
                    (config :twilio :token)
                    (config :twilio :number))
        server (run-jetty
                 (less.sexy.routing/create-handler store twilio)
                 (config :server))]
    (reset! (get-in system [:storage :store]) store)
    (-> system
      (assoc-in [:storage :shutdown]
                (storage/init! store (config :storage)))
      (assoc-in [:server :server] server))))

(defn stop!
  "Performs side effects to shut down the system and release its resources.
  Returns an updated instance of the system."
  [system]

  (.stop (get-in system [:server :server]))
  ((get-in system [:storage :shutdown]))
  (reset! (get-in system [:storage :store]) nil)
  (-> system
    (assoc-in [:server :server] nil)
    (assoc-in [:server :twilio] nil)
    (assoc-in [:storage :shutdown] nil)))

(defn -main [] (start! (system)))

