(ns krad.consts)

#_(def datascript-schema {:grapheme/name {:db/unique :db.unique/identity
                                        :db/cardinality :db.cardinality/one}
                        :grapheme/deps {:db/valueType :db.type/ref
                                        :db/cardinality :db.cardinality/many}})

(def unique {:db/unique :db.unique/identity})
(def one {:db/cardinality :db.cardinality/one})
(def refval {:db/valueType :db.type/ref})
(def many {:db/cardinality :db.cardinality/many})

(def schema {; grapheme will have only one name, and that name identifies only it
             :grapheme/name (merge unique one)
             ; grapheme may have multiple requirement-sets
             :grapheme/req-set (merge many refval)

             ; multiple graphemes comprise requirement
             :req-set/requirement (merge many refval) 
             ; many users may downvote this
             :req-set/downvote (merge many refval)

             ; a nickname can have only one nickname, but it can be reused (a
             ; nickname with two names is two nicknames)
             :nick/name (merge one)
             ; this nickname points to just one grapheme
             :nick/grapheme (merge one refval)
             ; this namename can be used by multiple users
             :nick/user (merge many refval)

             ; a user can have only one nickname and that name uniquely
             ; identifies them
             :user/name (merge unique one)})
