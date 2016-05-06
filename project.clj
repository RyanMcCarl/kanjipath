(defproject krad "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.5.1"]
                 [re-frame "0.7.0"]
                 [secretary "1.2.3"]
                 [garden "1.3.2"]
                 [compojure "1.5.0"]
                 [ring "1.4.0"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-garden "0.2.6"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler krad.handler/handler}

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
       :source-paths ["src/cljs"]
       :figwheel     {:on-jsload "krad.core/mount-root"}
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
       :source-paths ["src/cljs"]
       :compiler     {:main            krad.core
                      :output-to       "resources/public/js/compiled/app.js"
                      :optimizations   :advanced
                      :closure-defines {goog.DEBUG false}
                      :pretty-print    false}}]}}})
