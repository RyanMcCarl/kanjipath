(ns krad.db
  (:require [datascript.core :as d]
            [krad.consts :as consts]))

(def default-db
  {:name "re-frame"
   :abc-graphemes nil
   :dsdb (d/empty-db consts/datascript-schema)})
