(ns krad.handlers
    (:require [re-frame.core :as r]
              [krad.db :as kdb]
              [goog.net.XhrIo :as xhr]
              [cognitect.transit :as transit]
              [datascript.core :as d]))

(def json-reader (transit/reader :json))
(defn from-transit [s] (transit/read json-reader s))

(r/register-handler
 :initialize-db
 (fn  [_ _]
   (xhr/send "/abc"
             #(r/dispatch [:abc-received
                           {:status (-> % .-target .getStatus)
                            :content-type (-> % .-target (.getResponseHeader "Content-Type"))
                            :data (-> % .-target .getResponseText)}])
             "GET"
             nil
             #js {"Accept" "application/transit+json, application/json, */*"})
   (-> kdb/default-db)))

(r/register-handler
  :abc-received
  (fn [db [_ {:keys [status content-type data]}]]
    (if (= status 200)
      (let [obj (from-transit data)]
        (r/dispatch [:abc-to-dsdb obj])
        (assoc db :abc-graphemes obj))
      (do (js/alert "Failed to load Kanji ABC graphemes.")
          db))))

(r/register-handler
  :abc-to-dsdb
  (fn [db [_ {:keys [table] :as data}]]
    (let [conn (:dsdb db)
          txlog (d/transact! conn
                             (mapv (fn [s] {:entry/name s})
                                   (->> table
                                        flatten
                                        (map :grapheme/name)
                                        distinct)))]
      (assoc db :txlog txlog))))

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

