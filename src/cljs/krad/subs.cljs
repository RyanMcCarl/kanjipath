(ns krad.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as r]))

(r/register-sub :db (fn [db _] (reaction @db)))
(r/register-sub :name (fn [db _] (reaction (:name @db))))
(r/register-sub :active-panel (fn [db _] (reaction (:active-panel @db))))
; this just returns the DataScript database in conn
(r/register-sub :conn-db (fn [db _]
                                (reaction @(:conn @db))))

