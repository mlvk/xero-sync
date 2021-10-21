(ns xero-syncer.middleware.auth
  (:require [xero-syncer.config :refer [env]]))

(defn wrap-api-key-authorized-middleware
  [handler]
  (fn [request]
    (let [api-key (env :api-key)
          x-api-key (-> request :parameters :header :x-api-key)
          has-api-key? (boolean api-key)
          has-x-api-key? (boolean x-api-key)]
      (if (and has-api-key? has-x-api-key? (= x-api-key (env :api-key)))
        (handler request)
        {:status 401
         :body "You are not authorized with that api-key "}))))
