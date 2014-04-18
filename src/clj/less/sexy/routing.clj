(ns less.sexy.routing
  (:require [less.sexy.views.index :as index]
            [less.sexy.storage :as storage]
            [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.session :as session]
            [ring.middleware.params :as params]
            [ring.middleware.edn :as edn-middleware]
            [ring.util.response :as response]))

(defn my-routes [store]
  (routes
    (resources "/public")
    (GET "/" [] index/get-page)
    (not-found "404")))

(defn create-handler [store]
  (-> (my-routes store)
    (params/wrap-params)
    (edn-middleware/wrap-edn-params)
    (session/wrap-session {:store store})
    (cookies/wrap-cookies)))
