(ns less.sexy.views
  (:require [hiccup.core]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn base [{:keys [content scripts]}]
  (html5 [:html {:lang "en"}
          [:head
           [:meta {:charset "UTF-8"}]
           [:title "less.sexy"]
           (include-css "/public/css/reset.css"
                        "http://fonts.googleapis.com/css?family=Ubuntu+Mono"
                        "/public/css/app.css")
           (include-js "/public/js/track.js")]
          [:body
           [:div#header
            [:h1 "less.sexy"]]
           [:div#content-holder [:div#content content]]]
          (include-js "/public/js/jsedn.js"
                      "/public/js/main.js")
          (apply include-js scripts)]))
