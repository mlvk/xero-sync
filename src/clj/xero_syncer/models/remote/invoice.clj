(ns xero-syncer.models.remote.invoice
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [try+]]
            [tick.core :as t]
            [xero-syncer.config :refer [env]]
            [xero-syncer.models.local.company :as lc]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.local.sales-order :as lso]
            [xero-syncer.services.xero :as xero]))

(def xero-invoices-endpoint "https://api.xero.com/api.xro/2.0/Invoices/")

(defn find-invoice-by-xero-id
  [xero-id]
  (try+ (-> (client/get (str xero-invoices-endpoint (or xero-id "FORCE-XERO-NIL"))
                        {:headers (xero/generate-auth-headers)
                         :accept :json})
            :body
            (parse-string true)
            :Invoices
            (first))
        (catch [:status 403] error (log/error {:what "Couldn't find invoice by xero-id"
                                               :error error}))
        (catch [:status 404] error (log/error {:what "Couldn't find invoice by xero-id"
                                               :error error}))))

(defn find-invoice-by-invoice-number
  [invoice-number]
  (try+ (-> (client/get (str xero-invoices-endpoint (or invoice-number "FORCE-XERO-NIL"))
                        {:headers (xero/generate-auth-headers)
                         :accept :json})
            :body
            (parse-string true)
            :Invoices
            (first))
        (catch [:status 403] error (log/error {:what "Couldn't find invoice by invoice-number"
                                               :error error}))
        (catch [:status 404] error (log/error {:what "Couldn't find invoice by invoice-number"
                                               :error error}))))

(defn- calc-due-date
  [start-date terms]
  (->> (t/>> start-date
             (t/new-period terms :days))))

(defn- build-line-items-payload
  [order-items]
  (for [oi order-items]
    (let [unit-price (:order_item_unit_price oi)
          quantity (:order_item_quantity oi)
          line-amount (* unit-price quantity)
          item-code (:item_code oi)
          description (:item_description oi)
          account-code (-> env :accounting :default-sales-account)]

      {"ItemCode" item-code
       "Quantity" quantity
       "Description" description
       "UnitAmmount" unit-price
       "LineAmount" line-amount
       "AccountCode" account-code})))

(defn- local-order->xero-invoice-payload
  [{:keys [id order_number delivery_date]}]

  (let [company (lc/get-company-by-order-id id)
        due-date (calc-due-date delivery_date (:terms company))
        company-xero-id (:xero_id company)
        order-items (lso/get-order-items-by-order-id id)
        line-items (build-line-items-payload order-items)
        date-formatted (t/format "yyyy-MM-dd" delivery_date)
        due-date-formatted (t/format "yyyy-MM-dd" due-date)]

    {"Type" "ACCREC"
     "InvoiceNumber" order_number
     "Contact" {"ContactID" company-xero-id}
     "LineItems" line-items
     "Date" date-formatted
     "DueDate" due-date-formatted
     "Status" "SUBMITTED"}))

(defn update-invoice!
  "Update an invoice in xero based on local order data"
  [local-order-data xero-invoice-id]
  (let [body (generate-string (local-order->xero-invoice-payload local-order-data))
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]

    (try+
     (-> (client/post (str xero-invoices-endpoint xero-invoice-id)
                      payload)
         :body
         (parse-string true)
         :Invoices
         (first))
     (catch [:status 403] error (log/error {:what "Couldn't update invoice"
                                            :error error}))
     (catch [:status 404] error (log/error {:what "Couldn't update invoice"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Couldn't update invoice"
                                            :error error})))))

(defn create-invoice!
  "Create a new invoice in xero based on local order data"
  [local-order-data]
  (let [body (generate-string (local-order->xero-invoice-payload local-order-data))
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]
    (try+
     (-> (client/put (str xero-invoices-endpoint)
                     payload)
         :body
         (parse-string true)
         :Invoices
         (first))
     (catch [:status 404] error (log/error {:what "Couldn't create invoice"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Couldn't create invoice"
                                            :error error})))))

(defn upsert-invoices!
  "Update invoices in xero based on local sales-order data"
  [sales-orders]
  (let [body (generate-string {:Invoices (map local-order->xero-invoice-payload sales-orders)})
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]
    (try+
     (-> (client/post xero-invoices-endpoint
                      payload)
         :body
         (parse-string true)
         :Invoices)
     (catch [:status 403] error (log/error {:what "Couldn't update invoice"
                                            :error error}))
     (catch [:status 404] error (log/error {:what "Couldn't update invoice"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Couldn't update invoice"
                                            :error error})))))

(defn sync-local->remote!
  [local-record-data]

  (let [matched-record (or (find-invoice-by-xero-id (:xero_id local-record-data))
                           (find-invoice-by-invoice-number (:order_number local-record-data)))
        has-match? (boolean matched-record)
        remote-xero-id (:InvoiceNumber matched-record)]

    (if has-match?
      (update-invoice! local-record-data remote-xero-id)
      (create-invoice! local-record-data))))