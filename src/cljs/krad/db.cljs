(ns krad.db
  (:require [datascript.core :as d]
            [krad.consts :as consts]))

(def default-db
  {:name "re-frame"
   :abc-graphemes nil
   :dsdb (d/create-conn consts/datascript-schema)
   :txlog nil})
