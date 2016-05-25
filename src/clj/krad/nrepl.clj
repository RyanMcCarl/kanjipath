(ns krad.nrepl
  (:require [mount.core :refer [defstate] :as mount]
            [krad.config :refer [config]]
            [clojure.tools.nrepl.server :as nrepl]))

(defonce nrepl-server (atom nil))

(defn start-nrepl [{:keys [host port]}]
  (println "nrepl starting on port" port)
  (reset! nrepl-server (nrepl/start-server :bind host :port port)))

(defn stop-nrepl []
  (when-not (nil? nrepl-server)
    (nrepl/stop-server @nrepl-server)
    (reset! nrepl-server nil)))

(defstate nrepl
  :start (start-nrepl (:nrepl config))
  :stop (stop-nrepl))

