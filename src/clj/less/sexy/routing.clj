(ns less.sexy.routing
  (:require [clojure.core.async :as async :refer [>! go]]
            [less.sexy.views.index :as index]
            [less.sexy.numbers :as numbers]
            [less.sexy.twilio :refer [valid-twilio-request?]]
            [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.session :as session]
            [ring.middleware.params :as params]
            [ring.middleware.edn :as edn-middleware]
            [ring.util.response :as response]))

(defn twilio-resp [f]
  (do (f)
      {:status 200
       :headers {"Content-Type" "text/xml; charset=utf-8"}
       :body "<?xml version=\"1.0\" ?><Response></Response>"}))

(defn my-routes [nums chans twilio]
  (routes
    (resources "/public")
    (GET "/" [] (index/get-page))
    (POST "/" [number] (when-let [phone (numbers/add nums number)]
                         (index/add-number-page phone)))
    (POST "/" [number] (index/bad-number-page number))
    (POST "/delete" [number] (numbers/del nums number))
    (POST "/sms" [From Body :as req]
      (when (valid-twilio-request? twilio req)
        (cond
          (numbers/auth-pending? nums From)
          (twilio-resp #(numbers/auth-received nums From Body))

          (numbers/getp nums From)
          (twilio-resp #(numbers/fwd-sms nums From Body))

          :else nil)))
    (not-found "404")))

(defn create-handler [session nums chans twilio]
  (-> (my-routes nums chans twilio)
    (params/wrap-params)
    (edn-middleware/wrap-edn-params)
    (session/wrap-session session)
    (cookies/wrap-cookies)))
