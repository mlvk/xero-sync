(ns xero-syncer.models.remote.item
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [try+ throw+]]
            [xero-syncer.config :refer [env]]
            [xero-syncer.services.xero :as xero]))

(def xero-items-endpoint "https://api.xero.com/api.xro/2.0/Items/")

(defn local-item->xero-item-payload
  [{:keys [description
           default_price
           name
           is_purchased
           code
           is_sold]}]

  (cond-> {"Code" code
           "Name" name

           "IsSold" is_sold
           "IsPurchased" is_purchased}
    is_sold (assoc "Description" description)
    is_purchased (assoc "PurchaseDetails" {"UnitPrice" default_price
                                           "AccountCode" (-> env :accounting :default-cogs-account)}
                        "PurchaseDescription" description)
    is_sold (assoc "SalesDetails" {"UnitPrice" default_price
                                   "AccountCode" (-> env :accounting :default-sales-account)})))

(defn- has-status-attr-errors?
  [record]
  (= (:StatusAttributeString record) "ERROR"))

(defn upsert-items!
  "Update items in xero based on local items data"
  [items]
  (let [body (generate-string {:Items (map local-item->xero-item-payload items)})
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}
        results (try+
                 (-> (client/post (str xero-items-endpoint "?summarizeErrors=false")
                                  payload)
                     :body
                     (parse-string true)
                     :Items)
                 (catch [:status 403] error (log/error {:what :xero
                                                        :msg "Forbidden, cannot access this resource"
                                                        :error error}))
                 (catch [:status 404] error (log/error {:what :xero
                                                        :msg "Couldn't find resource"
                                                        :error error}))
                 (catch [:status 400] error (log/error {:what "Unknown error"
                                                        :error error})))

        has-errors? (some has-status-attr-errors? results)
        errors (map (fn [r] (select-keys r [:ValidationErrors])) results)]


    (if has-errors?
      (do
        (log/error {:what :item-sync
                    :msg "There was an error syncing an item"
                    :error errors})
        nil)
      results)))