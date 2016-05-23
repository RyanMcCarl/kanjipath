(ns krad.handler
  (:require [compojure.core :refer [GET defroutes]]
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
  )

; changes below this won't be caught by wrap-reload! Only changes to the routes
; above.
(def handler (-> #'routes
                 (wrap-restful-format ,,, )
                 (wrap-resource "public") ;; OR this (2 of 2) __2_of_2__
                 (wrap-defaults site-defaults)
                 wrap-reload))

