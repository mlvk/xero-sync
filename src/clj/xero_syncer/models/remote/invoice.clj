(ns xero-syncer.models.remote.invoice
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [try+]]
            [tick.core :as t]
            [xero-syncer.models.local.location :as ll]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.config :refer [env]]
            [xero-syncer.models.local.company :as lc]
            [xero-syncer.models.local.order :as lo]
            [xero-syncer.services.xero :as xero]))

(def xero-invoices-endpoint "https://api.xero.com/api.xro/2.0/Invoices/")

(defn- calc-due-date
  [start-date terms]
  (->> (t/>> start-date
             (t/new-period terms :days))))

(defn- build-line-items-payload
  [order-items]
  (for [oi order-items
        :let [quantity (:order_item_quantity oi)]
        :when (> quantity 0)]
    (let [unit-price (:order_item_unit_price oi)
          quantity (:order_item_quantity oi)
          line-amount (* unit-price quantity)
          item-code (:item_code oi)
          description (:item_description oi)
          account-code (-> env :accounting :default-sales-account)]
      {"ItemCode" item-code
       "Quantity" quantity
       "Description" description
       "UnitAmount" unit-price
       "LineAmount" line-amount
       "AccountCode" account-code})))

(defn- build-shipping-fee
  [shipping-fee]
  {"Quantity" 1
   "Description" "Shipping"
   "UnitAmount" shipping-fee
   "LineAmount" shipping-fee
   "AccountCode" (-> env :accounting :shipping-revenue-account)})

(defn- local-order->xero-invoice-payload
  [{:keys [id order_number delivery_date shipping]}]

  (let [company (lc/get-company-by-order-id id)
        location (ll/get-location-by-order-id id)
        location-code (:code location)
        due-date (calc-due-date delivery_date (:terms company))
        company-xero-id (:xero_id company)
        order-items (lo/get-order-items-by-order-id id)
        product-line-items (build-line-items-payload order-items)
        has-shipping-fee? (> shipping 0)
        shipping-line (build-shipping-fee shipping)
        final-line-items (if has-shipping-fee? (conj (vec product-line-items) shipping-line) product-line-items)
        date-formatted (t/format "yyyy-MM-dd" delivery_date)
        due-date-formatted (t/format "yyyy-MM-dd" due-date)]

    {"Type" "ACCREC"
     "InvoiceNumber" order_number
     "Reference" location-code
     "Contact" {"ContactID" company-xero-id}
     "LineItems" final-line-items
     "Date" date-formatted
     "DueDate" due-date-formatted
     "Status" "SUBMITTED"}))

(defn upsert-invoices!
  "Update invoices in xero based on local sales-order data"
  [sales-orders]
  (let [body (generate-string {:Invoices (map local-order->xero-invoice-payload sales-orders)})
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]
    (try+
     (-> (client/post (str xero-invoices-endpoint "?summarizeErrors=false")
                      payload)
         :body
         (parse-string true)
         :Invoices)
     (catch [:status 403] error (log/error {:what :xero
                                            :msg "Forbidden, cannot access this resource"
                                            :error error}))
     (catch [:status 404] error (log/error {:what :xero
                                            :msg "Couldn't find resource"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Unknown error"
                                            :error error})))))

