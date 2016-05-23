(ns krad.db
  (:require [datascript.core :as d]))

(def datascript-schema {:entry/name {:db/unique :db.unique/identity
                                     :db/cardinality :db.cardinality/one}
                        :entry/deps {:db/valueType :db.type/ref
                                     :db/cardinality :db.cardinality/many}})

(def default-db
  {:name "re-frame"
   :abc-graphemes nil
   :dsdb (d/create-conn datascript-schema)
   :txlog nil})
