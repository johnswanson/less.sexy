(defproject less.sexy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :plugins [[lein-ring "0.8.10"]
            [lein-cljsbuild "1.0.3"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [ring-mock "0.1.5"]
                                  [spyscope "0.1.4"]]
                   :injections  [(require 'spyscope.core)]}
             :production {:main less.sexy.system}}
  :source-paths ["src/clj"]
  :cljsbuild {:builds [{:source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [fogus/ring-edn "0.1.0"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring-server "0.3.1"]
                 [compojure "1.1.6"]
                 [org.clojure/tools.reader "0.8.4"]
                 [hiccup "1.0.5"]
                 [com.twilio.sdk/twilio-java-sdk "3.4.1"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"})
