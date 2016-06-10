(ns krad.css
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :as s]))

(defstyles screen
  [:td {:min-width "5em"}]
  [:thead {:font-weight "bold"}]
  [:td:first-child {:font-weight "bold"}]
  [:table {:border-collapse "collapse"}]
  [:table :thead :td {:border "1px solid rgb(240,240,240)"}]
  [:div.graphemes-abc {:display "flex"
                       :flex-wrap "wrap"}]
)
