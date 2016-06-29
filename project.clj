(defproject krad "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [reagent "0.6.0-alpha2"]
                 [re-frame "0.7.0"]
                 [secretary "1.2.3"]
                 [garden "1.3.2"]
                 [compojure "1.5.0"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [cljs-ajax "0.5.4"] ;?
                 [org.clojure/core.async "0.2.374"]
                 ;[com.datomic/datomic-free "0.9.5359"]
                 [datascript "0.15.0"]
                 [criterium "0.4.4"]
                 [ring-middleware-format "0.7.0"]
                 [mount "0.1.10"]
                 [http-kit "2.2.0-alpha1"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [com.taoensso/sente "1.8.1"]
                 ]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljc"]

  :main krad.core

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-garden "0.2.6"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   krad.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}

    :profiles
  {:dev
   {:plugins [[lein-figwheel "0.5.2"]]
    :cljsbuild
    {:builds
     [{:id           "dev"
       :source-paths ["src/cljs" "src/cljc"]
       :figwheel     {:on-jsload "krad.core/mount-root"
                      :websocket-url "wss://localhost:34490/figwheel-ws"
                      }
       :compiler     {:main                 krad.core
                      :output-to            "resources/public/js/compiled/app.js"
                      :output-dir           "resources/public/js/compiled/out"
                      :asset-path           "js/compiled/out"
                      :source-map-timestamp true}}

]}}

   :prod
   {:cljsbuild
    {:builds
     [{:id           "min"
       :source-paths ["src/cljs" "src/cljc"]
       :compiler     {:main            krad.core
                      :output-to       "resources/public/js/compiled/app.js"
                      :optimizations   :advanced
                      :closure-defines {goog.DEBUG false}
                      :pretty-print    false}}]}}})
