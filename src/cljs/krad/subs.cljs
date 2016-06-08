(ns krad.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as r]))

(r/register-sub :db (fn [db _] (reaction @db)))
(r/register-sub :name (fn [db _] (reaction (:name @db))))
(r/register-sub :active-panel (fn [db _] (reaction (:active-panel @db))))
(r/register-sub :abc-graphemes (fn [db _] (reaction (:abc-graphemes @db))))
(r/register-sub :conn (fn [db _]
                        (let [heartbeat-sub (reaction (:conn-heartbeat @db))]
                          (reaction [(:conn @db) @heartbeat-sub]))))

