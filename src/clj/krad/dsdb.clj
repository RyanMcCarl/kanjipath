(ns krad.dsdb
  (require [datascript.core :as d]
           [clojure.string :as string]
           [krad.abc :as abc]
           [krad.consts :as consts]
           [mount.core :refer [defstate]]
           [krad.abc :refer [abc-state]]))

(defonce conn (d/create-conn consts/schema))

(defn start-dsdb []
  (println "Creating datascript server")
  (d/reset-conn! conn (d/empty-db consts/schema))
  (d/transact! conn (flatten abc/table-origin))
  (d/transact! conn [{:abc/groups abc/groups}])
  conn)

(defstate dsdb-state
  :start (start-dsdb))

