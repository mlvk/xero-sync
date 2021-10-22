(ns xero-syncer.routes.services
  (:require [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [throw+]]
            [xero-syncer.middleware.auth :as auth]
            [xero-syncer.middleware.formats :as formats]
            [xero-syncer.middleware.logger :as logger]
            [xero-syncer.services.scheduler :as scheduler-service]
            [xero-syncer.services.syncer :as syncer-service]
            [xero-syncer.services.xero :as xero]
            [xero-syncer.syncers.company :as company-syncer]
            [xero-syncer.syncers.item :as item-syncer]
            [xero-syncer.syncers.sales-order :as sales-order-syncer]
            [xero-syncer.utils.health-checks :as health]))

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
                 ;;  Logging
                 logger/wrap-logger-middleware]}

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

   ["/p"
    {:parameters {:header {:x-api-key string?}}
     :middleware [auth/wrap-api-key-authorized-middleware]}
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
                             (throw+ {:what :api-error
                                      :msk (str "No action found matching " action)}))

                           {:code 200
                            :body {:msg (str "Performed syncer " action)}}))}}]]

    ["/sync"
     ["/items"
      {:post {:parameters {:body {:ids vector?}}
              :handler (fn [request]
                         (let [ids (-> request :parameters :body :ids)]
                           (item-syncer/force-sync-items ids)
                           {:code 200
                            :body {:msg (str "Performed force sync for items " ids)}}))}}]

     ["/companies"
      {:post {:parameters {:body {:ids vector?}}
              :handler (fn [request]
                         (let [ids (-> request :parameters :body :ids)]
                           (company-syncer/force-sync-companies ids)
                           {:code 200
                            :body {:msg (str "Performed force sync for companies " ids)}}))}}]

     ["/sales-orders"
      {:post {:parameters {:body {:ids vector?}}
              :handler (fn [request]
                         (let [ids (-> request :parameters :body :ids)]
                           (sales-order-syncer/force-sync-sales-orders ids)
                           {:code 200
                            :body {:msg (str "Performed force sync for companies " ids)}}))}}]

     ["/batch"
      {:post {:parameters {:body {:model string?}}
              :handler (fn [request]
                         (let [model (-> request :parameters :body :model)]
                           (case model
                             "item" (item-syncer/force-sync-all-items)
                             "company" (company-syncer/force-sync-all-companies)
                             (throw+ {:what :api-error
                                      :msk (str "No model found matching " model)}))
                           {:code 200
                            :body {:msg (str "Performed force sync for " model)}}))}}]]

    ["/health-check"
     {:get {:handler #'health/health-check}}]]

   ["/oauth"
    {:get {:handler (fn [{:keys [query-params]}]
                      (let [code (get query-params "code")]
                        (xero/connect! code)
                        {:status 200
                         :body {:msg "ok"}}))}}]])