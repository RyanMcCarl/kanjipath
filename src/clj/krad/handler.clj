(ns krad.handler
  (:require [mount.core :refer [defstate]]
            [krad.config :refer [config]]
            [org.httpkit.server :as http-kit]
            [compojure.core :refer [GET POST defroutes routes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.resource :refer [wrap-resource]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [krad.abc :refer [abc-state]]
            [krad.dsdb :as dsdb :refer [dsdb-state]]
            [datascript.core :as d]
            [clojure.core.async :as a]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

;; sente
(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(a/go-loop []
           (println "Server received: " (a/<! ch-chsk))
           (recur))
;; continue

(defroutes my-routes
  (GET "/hi" [] (str "hi "))
  (GET "/test" [] {:body {:foo "bar" :baz [12 -412.12]}}) ; wrap-restful-format needs the body in a map

  (GET "/ds/abc" [req] (pr-str @dsdb-state))
  (GET "/abc" [] {:body abc-state})
  ; try going to /posts?title=Life&author=Me
  ; without wrap-defaults, you won't see "Life" or "Me".
  (GET "/posts" req
       (let [title (get (:params req) :title)
             author (get (:params req) :author)]
         (str "Title: " title ", Author: " author)))

  ; (route/resources "/")  ;; either THIS (1 of 2: search for __2_of_2__)
  (route/not-found "Not found"))

(defroutes sente-routes
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req)))

; changes below this won't be caught by wrap-reload! Only changes to the my-routes
; above.
(defn wrap-dir-index [handler]
  (fn [req]
    (handler
     (update-in req
                [:uri]
                #(if (= "/" %) "/index.html" %)))))

(def handler (routes
               (-> #'sente-routes
                   (wrap-defaults api-defaults)
                   ring.middleware.keyword-params/wrap-keyword-params
                   ring.middleware.params/wrap-params
                   )
               (-> #'my-routes
                   (wrap-restful-format ,,, )
                   (wrap-resource "public") ;; OR this (2 of 2) __2_of_2__
                   (wrap-defaults site-defaults)
                   wrap-dir-index
                   wrap-reload)))

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


