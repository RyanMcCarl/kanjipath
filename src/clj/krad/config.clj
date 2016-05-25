(ns krad.config
  (:require [mount.core :refer [defstate]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn load-config []
  (-> "config.edn"
      io/resource
      slurp
      edn/read-string))

(defstate config
  :start (load-config))

