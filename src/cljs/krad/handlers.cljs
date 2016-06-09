(ns krad.handlers
  (:require [re-frame.core :as r]
            [krad.db :as db]
            [krad.consts :as consts]
            [goog.net.XhrIo :as xhr]
            [cognitect.transit :as transit]
            [datascript.core :as d]))

(def json-reader (transit/reader :json))
(defn from-transit [s] (transit/read json-reader s))

(r/register-handler
 :initialize-db
 (fn  [_ _]
   (let [db db/default-db]
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
    (let [hz-coll ((:hz db) "eavt")]
      ; set up watch: incoming JS objects get dispatched
      (-> hz-coll
          .watch
          (.subscribe (fn [eavts] (r/dispatch [:datoms-from-horizon eavts]))))
      (-> db
          (assoc :hz-coll hz-coll)))))

(defn js-eavt-to-datom [obj]
  (d/datom (aget obj "e")
           (keyword (aget obj "a"))
           (aget obj "v")
           (aget obj "t")))

(r/register-handler
  :datoms-from-horizon
  (fn [db [_ eavts]]
    (println "Updating EAVTs from Horizon!" eavts)
    (-> db
        (assoc :conn (d/conn-from-datoms (map js-eavt-to-datom
                                              eavts)
                                         consts/schema)))))

; Right now there's only one way to initially hydrate RethinkDB with Kanji ABC
; EAVTs: from ClojureScript REPL:
; $ lein figwheel dev
; then
; => (do (require '[re-frame.core :as r]) (r/dispatch [:request-dsdb]))
(r/register-handler
  :request-dsdb
  (fn [db _]
    (xhr/send "/kanji-abc-datascript.edn"
              #(r/dispatch [:abc-dsdb-received
                            {:status (-> % .-target .getStatus)
                             :content-type (-> % .-target (.getResponseHeader "Content-Type"))
                             :data (-> % .-target .getResponseText)}]))
    db))

(extend-type Keyword
  IEncodeJS
  (-clj->js [s] (subs (str s) 1)))
(defn eav-to-map [v]
  (apply hash-map (interleave ["e" "a" "v"] (take 3 v))))
(defn eavt-to-map [v]
  (apply hash-map (interleave ["e" "a" "v" "t"] (take 4 v))))
(defn eavts-to-js-arrays [db]
  (clj->js (map eavt-to-map db)))
(defn eavs-to-js-arrays [db]
  (clj->js (map eav-to-map db)))



(defn remove-datoms-in-horizon [coll datoms]
  (when (not (empty? datoms))
    (-> (.apply (.-findAll coll) coll datoms)
        (.fetch)
        (.subscribe #(do (println "Looked for:" datoms " and removing:" %)
                       (-> coll (.removeAll %)))
                    #(println "ERROR in findAlls" %)))))

(defn store-datoms-in-horizon [coll datoms]
  (when (not (empty? datoms))
    (-> coll
      (.store datoms)
      (.subscribe #(println "Success writing to RethinkDB:" %)
                  #(println "ERROR writing to RethinkDB:" %)))))

(r/register-handler
  :submit-transaction
  (fn [db [_ tx]]
    (let [conn (:conn db)
          tx-report (d/with @conn tx)
          tx-data (:tx-data tx-report)
          {add true
           retract false} (group-by #(nth % 4) tx-data)
          coll (:hz-coll db)]
      (remove-datoms-in-horizon coll (eavs-to-js-arrays retract))
      (store-datoms-in-horizon coll (eavts-to-js-arrays add))
      db
      )))

(r/register-handler
  :abc-dsdb-received
  (fn [db [_ {:keys [status content-type data]}]]
    (if (= status 200)
      (let [full-dsdb (cljs.reader/read-string data)
            datoms (eavts-to-js-arrays full-dsdb)]
        (store-datoms-in-horizon (:hz-coll db) datoms)
        db)
      (do (js/alert "Failed to load Kanji ABC graphemes.")
          db))))

(r/register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

; unused because nothing is ever transacted onto DataScript db. `d/with` tells
; us the tx-data (datoms to add & retract) and we send that to Horizon/RethinkDB
; and only through there make it back to DataScript.
(r/register-handler
  :transact-dsdb
  (fn [db [_ transaction]]
    (let [conn (:dsdb db)
          txlog (d/transact! conn transaction)]
      (assoc db :txlog txlog))))

(defn create-new-req-set [db grapheme required-graphemes]
  (-> (d/with db 
        [{:db/id [:grapheme/name grapheme]
          :grapheme/req-set -123} ; any fixed <0 int (>0 are entids)
         {:db/id -123
          :req-set/requirement (mapv #(vector :grapheme/name %)
                                     required-graphemes)}])
      :tx-data))
