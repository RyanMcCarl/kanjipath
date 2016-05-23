(ns krad.css
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :as s]))

(defstyles screen
  #_[:body {:color "red"}]
  [:td {:min-width "5em"}]
  [:thead {:font-weight "bold"}]
  [:td:first-child {:font-weight "bold"}]
  [:table {:border-collapse "collapse"}]
  [:table :thead :td {:border "1px solid rgb(240,240,240)"}]
  ;[:td.origin-jouyou::after {:content "J"}]
  ;[(s/td ".origin-jouyou" s/after) {:content "\"J\""}]
)
