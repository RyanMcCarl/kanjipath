(ns krad.core
  (:require [mount.core :refer [defstate] :as mount]
            [krad.config :refer [config]]
            [krad.dsdb] ; mount will start datascript server
            [krad.nrepl] ; mount will start nrepl server
            [krad.handler] ; mount will start webserver
            [clojure.tools.nrepl.server :as nrepl])
  (:gen-class))

(defn -main [& args]
  (mount/start))
