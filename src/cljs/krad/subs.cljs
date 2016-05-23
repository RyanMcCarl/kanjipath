(ns krad.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as r]))

(r/register-sub :name (fn [db] (reaction (:name @db))))
(r/register-sub :active-panel (fn [db _] (reaction (:active-panel @db))))
(r/register-sub :abc-graphemes (fn [db _] (reaction (:abc-graphemes @db))))
(r/register-sub :dsdb (fn [db _] (let [conn-sub (reaction (:dsdb @db))
                                       txlog-sub (reaction (:txlog @db))]
                                   ; txlog-sub gets triggered at every
                                   ; transaction. conn-sub never changes since
                                   ; the atom remains the same. So we need both,
                                   ; and finally, we need to deref the
                                   ; Datascript atom so the third reaction won't
                                   ; be cache-broken (because of, again, the
                                   ; unchanging atom).
                                   (reaction (if @txlog-sub @@conn-sub)))))

