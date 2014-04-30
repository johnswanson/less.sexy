(ns less.sexy.views.index
  (:require [less.sexy.views :as views]
            [hiccup.core]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.util :refer [escape-html]]))

(declare error bad-number-error)

(defn index-content
  ([err] [:div.content
          (error err)
          [:form {:method "post"}
           [:div.numbers
            [:input {:type "text" :name "number" :placeholder "xxx xxx-xxxx"}]
            [:button {:type "submit"} "submit"]]]
          [:form {:method "post"
                  :action "/delete"}
           [:div.numbers
            [:input {:type "text" :name "number" :placeholder "xxx xxx-xxxx"}]
            [:button {:type "submit"} "delete"]]]])
  ([] (index-content nil)))

(defn add-number-content [number]
  [:div.content
   [:p (format "Awesome, authorization text sent to %s" number)]])

(defn- error [[t & args]]
  (case t
    :bad-number (apply bad-number-error args)
    nil))

(defn- bad-number-error [number]
  [:div.error
   [:h2 "Number error!"]
   [:p (format "We couldn't text %s." (escape-html number))]
   [:h3 "Check that:"]
   [:ul
    [:li "The number is a valid (US-only) number"]
    [:li "You typed the number correctly."]]])

(defn get-page []
  (views/base {:content (index-content)}))

(defn add-number-page [phone]
  (views/base {:content (add-number-content (:orig-number phone))}))

(defn bad-number-page [number]
  (views/base {:content (index-content [:bad-number number])}))

