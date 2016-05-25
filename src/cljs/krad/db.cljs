(ns krad.db
  (:require [datascript.core :as d]
            [krad.dsdb :as dsdb]))

(def default-db
  {:name "re-frame"
   :abc-graphemes nil
   :dsdb (d/create-conn dsdb/datascript-schema)
   :txlog nil})
