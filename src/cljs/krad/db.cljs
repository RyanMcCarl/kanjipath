(ns krad.db
  (:require [datascript.core :as d]
            [krad.consts :as consts]
            [re-frame.core :as r]))

(def default-db
  {:name "re-frame"
   :abc-graphemes nil
   :hz nil; TODO JS externs for Horizon!
   :hz-coll nil
   :conn (d/create-conn consts/schema)})
