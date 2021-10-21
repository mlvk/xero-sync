(ns xero-syncer.routes.services
  (:require [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [xero-syncer.utils.health-checks :as health]
            [xero-syncer.services.scheduler :as scheduler-service]
            [xero-syncer.services.syncer :as syncer-service]
            [clojure.pprint :refer [pprint]]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [xero-syncer.middleware.logger :as logger]
            [slingshot.slingshot :refer [throw+ try+]]
            [xero-syncer.db.core :as db]
            [reitit.swagger-ui :as swagger-ui]
            [ring.util.http-response :refer :all]
            [xero-syncer.middleware.auth :as auth]
            [xero-syncer.middleware.formats :as formats]
            [xero-syncer.services.xero :as xero]))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware
                 ;;  Auth
                 auth/wrap-api-key-authorized-middleware
                 ;;  Logging
                 logger/wrap-logger-middleware]
    :parameters {:header {:x-api-key string?}}}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]

   ["/status"
    {:get {:handler (fn [_]
                      {:code 200
                       :body {:xero (xero/health-check)
                              :services {:scheduler {:active-schedules (scheduler-service/current-schedules)}}}})}}]

   ["/services"
    ["/scheduler"
     {:post {:parameters {:body {:action string?}}
             :handler (fn [request]
                        (let [action (-> request :parameters :body :action)]
                          (case action
                            "start" (syncer-service/start-schedules)
                            "stop" (syncer-service/stop-schedules)
                            "restart" (syncer-service/restart-schedules)
                            (throw+ {:what :missing-action
                                     :msk (str "No action found matching " action)}))

                          {:code 200
                           :body {:msg (str "Performed syncer " action)}}))}}]]
   ["/health-check"
    {:get {:handler #'health/health-check}}]

   ["/oauth"
    {:get {:handler (fn [{:keys [query-params]}]
                      (let [code (get query-params "code")]
                        (xero/connect! code)
                        {:status 200
                         :body {:msg "ok"}}))}}]])