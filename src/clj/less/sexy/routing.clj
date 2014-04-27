(ns less.sexy.routing
  (:require [clojure.core.async :as async :refer [>! go]]
            [less.sexy.views.index :as index]
            [less.sexy.numbers :as numbers]
            [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.session :as session]
            [ring.middleware.params :as params]
            [ring.middleware.edn :as edn-middleware]
            [ring.util.response :as response]))

(defn my-routes [nums chans]
  (routes
    (resources "/public")
    (GET "/" [] (index/get-page))
    (POST "/" [number] (when-let [phone (numbers/add nums number)]
                         (index/add-number-page phone)))
    (POST "/" [number] (index/bad-number-page number))
    (POST "/delete" [number] (numbers/del nums number))
    (POST "/sms" [From Body]
      (numbers/auth-received nums From Body))
    (not-found "404")))

(defn create-handler [session nums chans]
  (-> (my-routes nums chans)
    (params/wrap-params)
    (edn-middleware/wrap-edn-params)
    (session/wrap-session session)
    (cookies/wrap-cookies)))
