(ns less.sexy.views.index
  (:require [less.sexy.views :as views]
            [hiccup.core]
            [hiccup.page :refer [html5 include-css include-js]]))

(def index-content
  [:div.content
   [:h1 "less.sexy"]
   [:form {:method "post"}
    [:input {:type "text" :name "number"}]
    [:button {:type "submit"} "submit"]]
   [:form {:method "post"
           :action "/delete"}
    [:input {:type "text" :name "number"}]
    [:button {:type "submit"} "delete"]]])

(defn get-page [{:keys []}]
  (views/base {:content index-content}))
