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
  [:.group {:display "flex"
            :flex-wrap "wrap"
            :width "100%"}]
  [:.grapheme {:display "flex"
               :flex-wrap "wrap"
               :background-color "#fafafa"
               :margin-right "1em"
               :margin-bottom "0.25em"
               :padding "0.1em"}]
  [:.req-set {:background-color "#eee"
             :margin-right "0.5em"
             :margin-left "0.5em"}]
)
