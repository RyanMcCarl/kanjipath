(ns krad.db)

(def datascript-schema {:grapheme/name {:db/unique :db.unique/identity
                                     :db/cardinality :db.cardinality/one}
                        :grapheme/deps {:db/valueType :db.type/ref
                                     :db/cardinality :db.cardinality/many}})

