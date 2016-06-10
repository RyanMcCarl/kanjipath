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

             ; multiple graphemes comprise requirement
             :req-set/requirement (merge many refval) 
             ; requirement is for a single grapheme
             :req-set/grapheme (merge one refval)
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
; A grapheme has a name, and requirement sets. A requirement set has graphemes
; and users who downvoted it. A grapheme nickname has a name, a grapheme, and
; users who subscribe to it. A user has a name.
