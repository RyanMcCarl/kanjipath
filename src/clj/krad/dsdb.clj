(ns krad.dsdb
  (require [datascript.core :as d]
           [clojure.string :as string]
           [krad.abc :as abc]
           [krad.consts :as consts]))

(defonce conn (d/create-conn consts/datascript-schema))
(d/transact! conn (flatten abc/table-origin))


