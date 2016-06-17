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

(defn render-grapheme [conn-db {graph-name :grapheme/name :as grapheme}]
  (let [req-sets (d/q {:find '[?reqset (distinct ?reqset-grapheme-name)]
                       :where [['?reqset :req-set/grapheme [:grapheme/name graph-name]]
                               '[?reqset :req-set/requirement ?reqset-grapheme]
                               '[?reqset-grapheme :grapheme/name ?reqset-grapheme-name]]}
                      conn-db)
        req-sets (map (comp #(apply str %) second) req-sets)]
    [:div.grapheme
     [:span
      {:onClick #(r/dispatch [:grapheme-clicked graph-name]) :className graph-name}
      (make-grapheme-name graph-name)]
      (when-not (empty? req-sets) (pr-str req-sets))  "　"]))


(defn tabulate-graphemes-compact []
  (let [dsdb-sub (r/subscribe [:conn-db])]
    (fn []
      (let [dsdb @dsdb-sub
            groups (d/q '[:find ?v .
                          :where [_ :abc/groups ?v]]
                        dsdb)
            group-to-idx (apply hash-map (interleave groups
                                                     (range (count groups))))
            unsorted-graphemes (d/q '[:find [(pull ?e [*]) ...]
                                      :where
                                      [?e :grapheme/name _]]
                                    dsdb)
            graphemes-list (sort-by (juxt (comp group-to-idx :grapheme/abc-group)
                                          :grapheme/abc-number)
                                    unsorted-graphemes)
            graphemes-table (partition-by :grapheme/abc-group graphemes-list)
            ]
        (into [:div.graphemes-abc]
              (mapcat
                (fn [group-name graphemes]
                  (into [[:div.group {:key group-name} (str "(" group-name ")　")]]
                        (map #(render-grapheme dsdb %)
                             graphemes)))
                groups
                graphemes-table))))))

(defn test-ds []
  (let [dsdb-sub (r/subscribe [:conn-db])]
    (fn []
      (let [dsdb @dsdb-sub]
        (into [:div]
              (map (fn [l] [:div (str l)])
                   (map seq dsdb)))))))


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

;; home

(defn home-panel []
  (let [name (r/subscribe [:name])]
    (fn []
      [:div (str "Hello from " @name ". This is the Home Page.")
       [make-css]
       [:div [:a {:href "#/about"} "go to About Page"]]
       #_[tabulate-graphemes]
       [tabulate-graphemes-compact]
       [test-ds]])))


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

