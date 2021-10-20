(ns xero-syncer.models.remote.item
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [try+]]
            [xero-syncer.config :refer [env]]
            [xero-syncer.models.local.item :as li]
            [xero-syncer.services.xero :as xero]))

(def xero-items-endpoint "https://api.xero.com/api.xro/2.0/Items/")

(defn find-item-by-xero-id
  [xero-id]
  (try+ (-> (client/get (str xero-items-endpoint (if xero-id xero-id "FORCE-XERO-NIL"))
                        {:headers (xero/generate-auth-headers)
                         :accept :json})
            :body
            (parse-string true)
            :Items
            (first))
        (catch [:status 403] error (log/error {:what "Couldn't find item by item"
                                               :error error}))
        (catch [:status 404] error (log/error {:what "Couldn't find item by item"
                                               :error error}))))

(defn find-item-by-code
  [code]
  (try+
   (-> (client/get (str xero-items-endpoint (if code code "FORCE-XERO-NIL"))
                   {:headers (xero/generate-auth-headers)
                    :accept :json})
       :body
       (parse-string true)
       :Items
       (first))
   (catch [:status 403] error (log/error {:what "Couldn't find item by code"
                                          :error error}))
   (catch [:status 404] error (log/error {:what "Couldn't find item by code"
                                          :error error}))))

(defn local-item->xero-item-payload
  [{:keys [description
           default_price
           name
           is_purchased
           code
           is_sold]}]

  (cond-> {"Code" code
           "Name" name
           "Description" description
           "IsSold" is_sold
           "IsPurchased" is_purchased}
    is_purchased (assoc "PurchaseDetails" {"UnitPrice" default_price
                                           "AccountCode" (-> env :accounting :default-cogs-account)})
    is_sold (assoc "SalesDetails" {"UnitPrice" default_price
                                   "AccountCode" (-> env :accounting :default-sales-account)})))

(defn update-item!
  "Update an item in xero based on local item data"
  [local-item-data xero-item-id]
  (let [body (generate-string (local-item->xero-item-payload local-item-data))
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]

    (try+
     (-> (client/post (str xero-items-endpoint xero-item-id)
                      payload)
         :body
         (parse-string true)
         :Items
         (first))
     (catch [:status 403] error (log/error {:what "Couldn't update item"
                                            :error error}))
     (catch [:status 404] error (log/error {:what "Couldn't update item"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Couldn't update item"
                                            :error error})))))

(defn create-item!
  "Create a new item in xero based on local item data"
  [local-item-data]
  (let [body (generate-string (local-item->xero-item-payload local-item-data))
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]

    (try+
     (-> (client/put (str xero-items-endpoint)
                     payload)
         :body
         (parse-string true)
         :Items
         (first))
     (catch [:status 404] res res)
     (catch [:status 400] res res))))

(defn upsert-items!
  "Update items in xero based on local items data"
  [items]
  (let [body (generate-string {:Items (map local-item->xero-item-payload items)})
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]
    (try+
     (-> (client/post xero-items-endpoint
                      payload)
         :body
         (parse-string true)
         :Items)
     (catch [:status 403] error (log/error {:what "Couldn't update item"
                                            :error error}))
     (catch [:status 404] res res)
     (catch [:status 400] res res))))

(defn sync-local->remote!
  [local-item-data]
  (let [matched-remote-item (or (find-item-by-xero-id (:xero_id local-item-data))
                                (find-item-by-code (:code local-item-data)))
        has-match? (boolean matched-remote-item)
        xero-item-id (:ItemId matched-remote-item)]

    (if has-match?
      (update-item! local-item-data xero-item-id)
      (create-item! local-item-data))))

#_(upsert-items! (li/get-sell-items))