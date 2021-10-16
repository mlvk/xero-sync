(ns xero-syncer.routes.services
  (:require [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [clojure.pprint :refer [pprint]]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [xero-syncer.db.core :as db]
            [reitit.swagger-ui :as swagger-ui]
            [ring.util.http-response :refer :all]
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
                 multipart/multipart-middleware]}

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

   ["/oauth"
    {:get {:handler (fn [{:keys [query-params]}]
                      (let [code (get query-params "code")]
                        (xero/xero-code->auth-token! code)
                        {:status 200
                         :body {:msg "ok"}}))}}]])