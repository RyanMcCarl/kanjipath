(ns krad.handlers
  (:require [re-frame.core :as r]
            [clojure.string :as string]
            [krad.db :refer [default-db]]
            [krad.consts :as consts]
            [krad.data :as kdata]
            [goog.net.XhrIo :as xhr]
            [clojure.set :refer [difference]]
            [cognitect.transit :as transit]
            [datascript.core :as d]))

(def json-reader (transit/reader :json))
(defn from-transit [s] (transit/read json-reader s))

(r/register-handler
  :initialize-db
  (fn  [_ _]
    (let [db default-db
          hz (doto (js/Horizon. #js {"authType" "unauthenticated"})
               (.onReady #(r/dispatch [:hz-ready]))
               ; Horizon connects when collection first used. No: connect
               ; explicitly so we can have our collection in db.
               (.connect))
          has-auth-token (.hasAuthToken hz)]
      (-> hz
          (.currentUser)
          (.fetch)
          (.subscribe #(r/dispatch [:current-user-arriving %])))
      (mapv (fn [provider]
              (-> hz
                  (.authEndpoint provider)
                  (.subscribe #(r/dispatch
                                 [:auth-endpoint-arriving provider %]))))
            (keys (:auth-endpoints db)))
      (-> db
          (assoc :hz hz)
          (assoc :auth? has-auth-token)))))

(r/register-handler
  :current-user-arriving
  (fn [db [_ data]]
    (assoc db :current-user data)))

(r/register-handler
  :auth-endpoint-arriving
  (fn [db [_ provider endpoint]]
    (assoc-in db [:auth-endpoints provider] endpoint)))

(defn forward-to-auth [hz]
  (-> hz
      (.authEndpoint "github")
      (.subscribe #(set! window.location.pathname %))))

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
    (println "Updating EAVTs from Horizon!" #_eavts)
    (-> db
        (assoc :conn (d/conn-from-datoms (map js-eavt-to-datom
                                              eavts)
                                         consts/schema)))))


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
(defn new-req-set-to-transaction [grapheme required-graphemes]
  [{:req-set/requirement (mapv #(vector :grapheme/name %)
                               required-graphemes)
    :req-set/grapheme [:grapheme/name grapheme]}])

(defn test-transaction [db tx]
  (-> (d/with db tx)
      :tx-data))

(defn test-create-new-req-set [grapheme required-graphemes db]
  (test-transaction db (new-req-set-to-transaction grapheme required-graphemes)))

(defn submit-new-req-set [grapheme required-graphemes]
  (if (-> required-graphemes count (> 1))
    (r/dispatch [:submit-transaction
                       (new-req-set-to-transaction grapheme
                                                   required-graphemes)])))

(r/register-handler
  :grapheme-clicked
  (fn [{:as db :keys [grapheme-name grapheme-req-names]} [_ clicked-name]]
    (if (nil? grapheme-name)
      (assoc db :grapheme-name clicked-name)
      (if (= grapheme-name clicked-name)
        (do
          ; some side-effect function of grapheme-req-names
          (submit-new-req-set grapheme-name grapheme-req-names)

          (println (str grapheme-name
                        " has requirements: "
                        (string/join " "
                                     grapheme-req-names)))

          ; reset db values
          (-> db
              (assoc :grapheme-name (:grapheme-name default-db))
              (assoc :grapheme-req-names (:grapheme-req-names default-db))))
        (update db :grapheme-req-names conj clicked-name)))))

; Right now there's only one way to initially hydrate RethinkDB with Kanji ABC
; EAVTs: from ClojureScript REPL:
; $ lein figwheel dev
; then
#_(do
    (require '[re-frame.core :as r])
    (r/dispatch [:request-abc-dsdb])
    (r/dispatch [:add-kanji]))
(r/register-handler
  :add-kanji
  (fn [db _]
    (let [dsdb @(-> db :conn)
          already-in (set (d/q '[:find [?name ...]
                               :where
                               [_ :grapheme/name ?name]]
                             dsdb))
        make-tx (fn [input-set origin-kw]
                  (mapv (fn [k] {:grapheme/name k
                                 :grapheme/origins [origin-kw]})
                        (sort (difference input-set already-in))))
        joyo-tx (make-tx kdata/joyo :jouyou)
        jinmeiyo-tx (make-tx kdata/jinmeiyo :jinmeiyo)
        ]
    (r/dispatch [:submit-transaction (into joyo-tx jinmeiyo-tx)])
    db)))

(r/register-handler
  :request-abc-dsdb
  (fn [db _]
    (xhr/send "/kanji-abc-datascript.edn"
              #(r/dispatch [:abc-dsdb-received
                            {:status (-> % .-target .getStatus)
                             :content-type (-> % .-target (.getResponseHeader "Content-Type"))
                             :data (-> % .-target .getResponseText)}]))
    db))

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


