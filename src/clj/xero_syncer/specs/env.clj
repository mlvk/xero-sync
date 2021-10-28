(ns xero-syncer.specs.env
  (:require [malli.core :as m]
            [malli.error :as me]))

(def schema
  [:map
   [:db [:map
         [:name string?]
         [:user string?]
         [:password string?]
         [:host string?]
         [:port int?]]]

   [:xero [:map
           [:oauth-callback-uri string?]
           [:client-id string?]
           [:client-secret string?]
           [:tenant-name string?]]]

   [:accounting [:map
                 [:default-cogs-account int?]
                 [:default-sales-account int?]
                 [:shipping-revenue-account int?]]]

   [:cloudamqp-url string?]

   [:redistogo-url string?]

   [:api-key string?]])

(def env-validator
  (m/validator schema))

(defn is-valid?
  [env-vars]
  (env-validator env-vars))

(defn explain
  [env-vars]
  (-> (m/explain schema env-vars)
      (me/humanize)))