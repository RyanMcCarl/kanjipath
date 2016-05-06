(ns krad.handler
  (:require [compojure.core :refer [GET defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.resource :refer [wrap-resource]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes routes
  (GET "/hi" [] (str "hi "))

  ; try going to http://localhost:3449/posts?title=Life&author=Me
  ; without wrap-defaults, you won't see "Life" or "Me".
  (GET "/posts" req
       (let [title (get (:params req) :title)
             author (get (:params req) :author)]
         (str "Title: " title ", Author: " author)))
  
  #_(route/resources "/")  ;; either THIS (1 of 2: search for __2_of_2__)
  )

; changes below this won't be caught by wrap-reload! Only changes to the routes
; above.
(def handler (-> #'routes
                 (wrap-resource "public") ;; OR this (2 of 2) __2_of_2__
                 (wrap-defaults site-defaults)
                 wrap-reload))

