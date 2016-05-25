(ns krad.dsdb
  (require [datascript.core :as d]
           [clojure.string :as string]
           [krad.abc :as abc]))

(defonce conn (d/create-conn dsdb/datascript-schema))
(d/transact! conn (flatten abc/table-origin))


