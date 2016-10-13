(defproject om-next-e2e "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure            "1.8.0"]
                 [org.clojure/tools.logging      "0.3.1"]
                 [funcool/catacumba              "1.1.1"]
                 [com.stuartsierra/component     "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [org.clojure/core.async         "0.2.395"]
                 ;; client
                 [org.clojure/clojurescript  "1.9.229"]
                 [org.omcljs/om              "1.0.0-alpha46"]
                 [cljs-http                  "0.1.42"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [binaryage/devtools         "0.8.2"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :figwheel {:css-dirs ["resources/public/css"]}

  :repl-options {:init-ns user
                 :init (set! *print-length* 100)
                 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :main ^:skip-aot om-next-e2e.core

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [figwheel-sidecar            "0.5.8"]
                                  [com.cemerick/piggieback     "0.2.1"]]
                   :source-paths ["dev"]
                   :plugins [[lein-figwheel "0.5.8"]]}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    :target-path]

  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/cljs"]
             :figwheel {:on-jsload "om-next-e2e.core/mount-root"}
             :compiler {:main om-next-e2e.core
                        :output-to "resources/public/js/compiled/app.js"
                        :output-dir "resources/public/js/compiled/out"
                        :asset-path "js/compiled/out"
                        :source-map-timestamp true}}
            {:id "min"
             :source-paths ["src/cljs"]
             :compiler {:main om-next-e2e.core
                        :output-to "resources/public/js/compiled/app.js"
                        :optimizations :advanced
                        :closure-defines {goog.DEBUG false}
                        :pretty-print false}}]})
