(ns krad.db
  (:require [datascript.core :as d]
            [krad.consts :as consts]))

(def default-db
  {:name "re-frame"
   :abc-graphemes nil
   :conn (d/create-conn consts/schema)
   :conn-heartbeat 0})
