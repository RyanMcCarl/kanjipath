(ns krad.handlers
    (:require [re-frame.core :as r]
              [krad.db :as db]
              [goog.net.XhrIo :as xhr]
              [cognitect.transit :as transit]))

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
   (-> db/default-db)))

(r/register-handler
  :abc-received
  (fn [db [_ {:keys [status content-type data]}]]
    (if (= status 200)
      (assoc db :abc-graphemes (from-transit data))
      (do (js/alert "Failed to load Kanji ABC graphemes.")
          db))))

(r/register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))
