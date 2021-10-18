(ns xero-syncer.models.remote.item
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [try+]]
            [xero-syncer.config :refer [env]]
            [xero-syncer.services.xero :as xero]))

(defn generate-auth-headers
  []
  {:authorization (xero/generate-bearer-auth-header)
   :Xero-Tenant-Id (-> env :xero :tenant-id)})

(defn find-item-by-xero-id
  [xero-id]
  (try+ (-> (client/get (str "https://api.xero.com/api.xro/2.0/Items/" xero-id)
                        {:headers (generate-auth-headers)
                         :accept :json})
            :body
            (parse-string true)
            :Items)
        (catch [:status 404] {} nil)))

(defn find-item-by-code
  [code]
  (try+
   (-> (client/get (str "https://api.xero.com/api.xro/2.0/Items/" code)
                   {:headers (generate-auth-headers)
                    :accept :json})
       :body
       (parse-string true)
       :Items)
   (catch [:status 404] {} nil)))

(defn local-item->xero-item-payload
  [{:keys [:description
           :default_price
           :name
           :is_purchased
           :company_id
           :updated_at
           :xero_id
           :active
           :id
           :unit_of_measure
           :code
           :position
           :tag
           :sync_state
           :created_at
           :is_sold]}]

  (let [payload (cond-> {"Code" code
                         "Name" name
                         "IsSold" is_sold
                         "IsPurchased" is_purchased}
                  is_purchased (assoc "PurchaseDetails" {"UnitPrice" default_price
                                                         "AccountCode" (-> env :accounting :default-cogs-account)})
                  is_sold (assoc "SalesDetails" {"UnitPrice" default_price
                                                 "AccountCode" (-> env :accounting :default-sales-account)}))]
    (generate-string payload)))

(defn sync-local<-remote!
  [{:keys [local remote]}])


(defn update-item!
  "Update an item in xero based on local item data"
  [local-item-data xero-item-id]
  (let [opts {:headers (generate-auth-headers)
              :accept :json
              :body (local-item->xero-item-payload local-item-data)}]

    (try+
     (-> (client/post (str "https://api.xero.com/api.xro/2.0/Items/" xero-item-id)
                      opts)
         :body
         (parse-string true)
         :Items
         (first))
     (catch [:status 404] res res)
     (catch [:status 400] res res))))

(defn create-item!
  "Create a new item in xero based on local item data"
  [local-item-data]
  (let [opts {:headers (generate-auth-headers)
              :accept :json
              :body (local-item->xero-item-payload local-item-data)}]

    (try+
     (-> (client/put (str "https://api.xero.com/api.xro/2.0/Items")
                     opts)
         :body
         (parse-string true)
         :Items
         (first))
     (catch [:status 404] res res)
     (catch [:status 400] res res))))


(defn sync-local->remote!
  [local-item-data]

  (let [matched-item (or (find-item-by-xero-id (:xero_id local-item-data))
                         (find-item-by-code (:code local-item-data)))
        has-match? (boolean matched-item)
        xero-item-id (:ItemId matched-item)]

    (if has-match?
      (update-item! local-item-data xero-item-id)
      (create-item! local-item-data))))