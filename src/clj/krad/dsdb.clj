(ns krad.dsdb
  (require [datascript.core :as d]
           [clojure.string :as string]
           [krad.dsdb :as dsdb]
           [krad.abc :as abc]))

(defonce conn (d/create-conn dsdb/datascript-schema))

