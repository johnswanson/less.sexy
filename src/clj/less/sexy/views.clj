(ns less.sexy.views
  (:require [hiccup.core]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn base [{:keys [content scripts]}]
  (html5 [:html {:lang "en"}
          [:head
           [:meta {:charset "UTF-8"}]
           [:title "less.sexy"]
           [:link {:href "/public/css/app.css"
                   :rel "stylesheet"
                   :type "text/css"}]
           (include-js "/public/js/track.js")]
          [:body
           [:h1 "less.sexy"]
           [:div#content content]]
          (include-js "/public/js/main.js")
          (apply include-js scripts)]))
