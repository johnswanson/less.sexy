(ns less.sexy.views.index
  (:require [less.sexy.views :as views]
            [hiccup.core]
            [hiccup.page :refer [html5 include-css include-js]]))

(def index-content
  [:h1 "test"])

(defn get-page [{:keys []}]
  (views/base {:content index-content}))
