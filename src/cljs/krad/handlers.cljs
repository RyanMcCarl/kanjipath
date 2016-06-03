(ns krad.handlers
    (:require [re-frame.core :as r]
              [krad.db :as db]
              [goog.net.XhrIo :as xhr]
              [cognitect.transit :as transit]
              [datascript.core :as d]))

(def json-reader (transit/reader :json))
(defn from-transit [s] (transit/read json-reader s))

(r/register-handler
 :initialize-db
 (fn  [_ _]
   (xhr/send "/ds/abc"
             #(r/dispatch [:abc-dsdb-received
                           {:status (-> % .-target .getStatus)
                            :content-type (-> % .-target (.getResponseHeader "Content-Type"))
                            :data (-> % .-target .getResponseText)}]))
   (let [db db/default-db]
     (d/listen! (:conn db) #(r/dispatch [:conn-transacted]))
     db)))

(r/register-handler
  :conn-transacted
  (fn [db _]
    (update db :conn-heartbeat inc)))

(r/register-handler
  :abc-dsdb-received
  (fn [db [_ {:keys [status content-type data]}]]
    (if (= status 200)
      (let [full-dsdb (cljs.reader/read-string data)]
        (d/reset-conn! (:conn db) full-dsdb)
        (update db :conn-heartbeat inc))
      (do (js/alert "Failed to load Kanji ABC graphemes.")
          db))))



(r/register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(r/register-handler
  :transact-dsdb
  (fn [db [_ transaction]]
    (let [conn (:dsdb db)
          txlog (d/transact! conn transaction)]
      (assoc db :txlog txlog))))

