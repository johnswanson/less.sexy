(ns less.sexy.routing
  (:require [less.sexy.views.index :as index]
            [less.sexy.storage :as storage]
            [less.sexy.twilio :as twilio]
            [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.session :as session]
            [ring.middleware.params :as params]
            [ring.middleware.edn :as edn-middleware]
            [ring.util.response :as response]))

(defn my-routes [store twilio]
  (routes
    (resources "/public")
    (GET "/" [] index/get-page)
    (POST "/" [number] (storage/add-number! store number))
    (POST "/delete" [number] (storage/del-number! store number))
    (GET "/test" [] (twilio/send-sms twilio "+19073511000" "testbody"))
    (DELETE "/" [number] (storage/del-number! store number))
    (not-found "404")))

(defn create-handler [store twilio]
  (-> (my-routes store twilio)
    (params/wrap-params)
    (edn-middleware/wrap-edn-params)
    (session/wrap-session {:store store})
    (cookies/wrap-cookies)))
