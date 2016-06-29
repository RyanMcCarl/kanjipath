(ns krad.views
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [re-frame.core :as r]
            [garden.core :refer [css]]
            [clojure.string :as string]
            [datascript.core :as d]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]))

;; graphemes
(defn kw-vec-to-str [v]
  (string/join (map name v)))

(defn make-grapheme-name [s]
  (if (string/starts-with? s "U+")
    [:span.babelstone-han-pua (-> (subs s 2)
                                  (#(js/parseInt % 16))
                                  char
                                  str)]
    s))

(defn tabulate-grapheme-row [group graphemes origin-kw-to-char]
  (into [:tr [:td group]]
        (map (fn [grapheme]
               [:td
                (make-grapheme-name (:grapheme/name grapheme))
                [:sub (string/join (->> grapheme
                                        :grapheme/origins
                                        (map origin-kw-to-char)))]])
             graphemes)))

(defn render-req-set [required-graphemes grapheme]
  (let [out (string/join "," required-graphemes)]
    [:div.req-set {:key (str grapheme out)} out]))

(defn render-grapheme [req-lists {graph-name :grapheme/name :as grapheme}]
  [:div.grapheme
   {:key graph-name}
   [:span
    {:key (str "span-" graph-name)
     :onClick #(r/dispatch [:grapheme-clicked graph-name]) :className graph-name}
    (make-grapheme-name graph-name)]
   (map render-req-set req-lists)])

(defn db-to-reqs [dsdb]
  (let [q-results (d/q '[:find ?gn ?rs (distinct ?rg)
                         :where [?rs :req-set/grapheme ?g]
                         [?g :grapheme/name ?gn]
                         [?rs :req-set/requirement ?rsr]
                         [?rsr :grapheme/name ?rg]]
                       dsdb)
        results-map (group-by first q-results)
        results-map (into {} (map (fn [[k v]] [k (mapv #(nth % 2) v)])
                                  results-map))]
    results-map))

(defn tabulate-graphemes-compact []
  (let [dsdb-sub (r/subscribe [:conn-db])]
    (fn []
      (let [dsdb @dsdb-sub
            groups (d/q '[:find ?v .
                          :where [_ :abc/groups ?v]]
                        dsdb)
            group-to-idx (apply hash-map (interleave groups
                                                     (range (count groups))))
            group-to-idx (assoc group-to-idx nil 1e3)

            unsorted-graphemes (d/q '[:find [(pull ?e [*]) ...]
                                      :where
                                      [?e :grapheme/name _]]
                                    dsdb)
            graphemes-list (sort-by (juxt (comp group-to-idx :grapheme/abc-group)
                                          :grapheme/abc-number
                                          :grapheme/name)
                                    unsorted-graphemes)
            graphemes-table (partition-by :grapheme/abc-group graphemes-list)

            groups-all (map #(-> % first :grapheme/abc-group (or "_"))
                            graphemes-table)

            graphemes-and-requirements (db-to-reqs dsdb)
            ]
        [:div.graphemes-abc
         (map (fn [group-name graphemes]
                [:div.group
                 {:key group-name}
                 (str "(" group-name ")")
                 (map (fn [g] (render-grapheme (get graphemes-and-requirements
                                                    (:grapheme/name g))
                                               g))
                      graphemes)])
              groups-all
              graphemes-table)]))))

(defn test-ds []
  (let [dsdb-sub (r/subscribe [:conn-db])]
    (fn []
      (let [dsdb @dsdb-sub]
        (into [:div]
              (map (fn [l] [:div (str l)])
                   (map seq (take-last 50 dsdb))))))))


(defn make-css []
  (let [grapheme-sub (r/subscribe [:grapheme-names])]
    (fn []
      (let [str-to-class #(keyword (str "." %))
            [grapheme-name grapheme-req-names] @grapheme-sub]
        [:style (css (when grapheme-name [(str-to-class grapheme-name)
                                          {:color "red"}])
                     (conj (mapv str-to-class grapheme-req-names)
                           {:color "green"})
                     )]))))

(defn choose-login []
  (let [auth-endpoints-sub (r/subscribe [:auth-endpoints])]
    (fn []
      (let [auth-endpoints @auth-endpoints-sub]
        (into [:ul]
              (map (fn [[provider endpoint]] [:li
                                              "Log in with "
                                              [:a {:href endpoint}
                                               provider]]))
              auth-endpoints)))))

(defn auth-info []
  (let [auth?-sub (r/subscribe [:auth?])]
    (fn []
      (let [auth? @auth?-sub]
        [:div.auth
         (if auth?
           [:span "You are logged in!"]
           [choose-login])]))))

;; home

(defn home-panel []
  (let [name (r/subscribe [:name])]
    (fn []
      [:div ;(str "Hello from " @name ". This is the Home Page.")
       [make-css]
       ; [:div [:a {:href "#/about"} "go to About Page"]]
       [auth-info]
       [tabulate-graphemes-compact]
       #_[test-ds]])))


;; about

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a {:href "#/"} "go to Home Page"]]]))


;; main

(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :default [] [:div])

(defn show-panel
  [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (r/subscribe [:active-panel])]
    (fn []
      [show-panel @active-panel])))

