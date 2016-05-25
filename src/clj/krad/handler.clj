(ns krad.handler
  (:require [mount.core :refer [defstate]]
            [krad.config :refer [config]]
            [org.httpkit.server :as http-kit]
            [compojure.core :refer [GET defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.resource :refer [wrap-resource]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [krad.abc :as abc]))

(defroutes routes
  (GET "/hi" [] (str "hi "))
  (GET "/test" [] {:body {:foo "bar" :baz [12 -412.12]}}) ; wrap-restful-format needs the body in a map

  (GET "/abc" [] {:body {:origin-kw-to-char abc/origin-kw-to-char
                         :groups abc/groups
                         :table abc/table-origin}})
  ; try going to http://localhost:3449/posts?title=Life&author=Me
  ; without wrap-defaults, you won't see "Life" or "Me".
  (GET "/posts" req
       (let [title (get (:params req) :title)
             author (get (:params req) :author)]
         (str "Title: " title ", Author: " author)))

  #_(route/resources "/")  ;; either THIS (1 of 2: search for __2_of_2__)
  (route/not-found "Not found")
  )

; changes below this won't be caught by wrap-reload! Only changes to the routes
; above.
(defn wrap-dir-index [handler]
  (fn [req]
    (handler
     (update-in req
                [:uri]
                #(if (= "/" %) "/index.html" %)))))

(def handler (-> #'routes
                 (wrap-restful-format ,,, )
                 (wrap-resource "public") ;; OR this (2 of 2) __2_of_2__
                 (wrap-defaults site-defaults)
                 wrap-dir-index
                 wrap-reload))

(defonce http-kit-shutdown-fn (atom nil))

(defn start-webserver [{:keys [www]}]
  (println "Starting server on port" (:port www))
  (reset! http-kit-shutdown-fn
          (http-kit/run-server handler {:port (:port www)})))

(defn stop-webserver []
  (when-not (nil? @http-kit-shutdown-fn)
    (@http-kit-shutdown-fn :timeout 500)
    (reset! http-kit-shutdown-fn nil)))

(defstate webserver
  :start (start-webserver config)
  :stop (stop-webserver))


