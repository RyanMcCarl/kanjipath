(ns krad.views
    (:require [re-frame.core :as r]
              [clojure.string :as string]))

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
                ;{:className "babelstone-han"}
                (make-grapheme-name (:grapheme/name grapheme))
                [:sub (string/join (->> grapheme
                                        :grapheme/origins
                                        (map origin-kw-to-char)))]])
             graphemes)))

(defn tabulate-graphemes []
  (let [abc-graphemes (r/subscribe [:abc-graphemes])]
    (fn []
      (if @abc-graphemes
        (let [{:keys [table groups origin-kw-to-char]} @abc-graphemes]
          [:div.graphemes-main
           [:table
            [:thead (into [:tr [:td "Group"]]
                          (map (fn [n] [:td n])
                               (range 1 27)))]

            (into [:tbody]
                  (map tabulate-grapheme-row
                       groups
                       table
                       (repeat origin-kw-to-char)))]])))))

;; home

(defn home-panel []
  (let [name (r/subscribe [:name])]
    (fn []
      [:div (str "Hello from " @name ". This is the Home Page.")
       [:div [:a {:href "#/about"} "go to About Page"]]
       [tabulate-graphemes]])))


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
