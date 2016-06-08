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
   (let [db db/default-db]
     (println "Listening for DataScript changes")
     (d/listen! (:conn db) #(r/dispatch [:conn-transacted]))
     (-> db
         (assoc :hz (doto (js/Horizon.)
                      (.onReady #(r/dispatch [:hz-ready]))
                      ; Horizon connects when collection first used. No: connect
                      ; explicitly so we can have our collection in db.
                      (.connect)))))))

(r/register-handler
  :hz-ready
  (fn [db _]
    (println "Horizon ready")
    (assoc db :hz-coll ((:hz db) "eavt"))))

(r/register-handler
  :conn-transacted
  (fn [db _]
    (update db :conn-heartbeat inc)))

; Right now there's only one way to initially hydrate RethinkDB with Kanji ABC
; EAVTs: from ClojureScript REPL:
; $ lein figwheel dev
; then
; => (require '[re-frame.core :as r]) (r/dispatch [:request-dsdb])
(r/register-handler
  :request-dsdb
  (fn [db _]
    (xhr/send "/kanji-abc-datascript.edn"
              #(r/dispatch [:abc-dsdb-received
                            {:status (-> % .-target .getStatus)
                             :content-type (-> % .-target (.getResponseHeader "Content-Type"))
                             :data (-> % .-target .getResponseText)}]))
    db))

(defn datom-to-obj [v]
  (apply hash-map (interleave ["e" "a" "v" "t"] (take 4 v))))

(r/register-handler
  :abc-dsdb-received
  (fn [db [_ {:keys [status content-type data]}]]
    (if (= status 200)
      (let [full-dsdb (cljs.reader/read-string data)
            datoms (clj->js (map datom-to-obj (map #(take 4 %) full-dsdb)))]
        (-> (:hz-coll db)
            (.store datoms)
            (.subscribe #(println "Success writing to RethinkDB:" %)
                        #(println "ERROR writing to RethinkDB:" %)))
        db
        #_(d/reset-conn! (:conn db) full-dsdb)
        #_(update db :conn-heartbeat inc))
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

