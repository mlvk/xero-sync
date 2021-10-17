(ns xero-syncer.services.xero
  (:require [clj-http.client :as client]
            [clojure.pprint :refer [pprint]]
            [slingshot.slingshot :refer [throw+ try+]]
            [cheshire.core :refer [generate-string parse-string]]
            [ring.util.http-response :refer :all]
            [postmortem.core :as pm]
            [tick.core :as t]
            [xero-syncer.config :refer [env]])
  (:import java.util.Base64))

(def xero-token-endpoint "https://identity.xero.com/connect/token")

(defn encodeB64 [to-encode]
  (.encodeToString (Base64/getEncoder) (.getBytes to-encode)))

(defonce auth-token (atom nil))

(defn- reset-auth-token!
  [token]
  (reset! auth-token (with-meta token {:created (t/now)})))

(def grant-type-lookup-table
  {:authorization-code "authorization_code"
   :refresh "refresh_token"})

(defn- generate-auth-header
  []
  (str
   "Basic "
   (encodeB64
    (str
     (-> env :xero :client-id)
     ":"
     (-> env :xero :client-secret)))))

(defn- refresh-token-request!
  [refresh-token]
  (-> (client/post xero-token-endpoint {:headers {:authorization (generate-auth-header)}
                                        :form-params {:grant_type (:refresh grant-type-lookup-table)
                                                      :refresh_token refresh-token}
                                        :as :auto
                                        :accept :json})
      :body))

(defn refresh-token!
  [refresh-token]
  (reset-auth-token! (refresh-token-request! refresh-token)))

(defn token-time-remaining
  [token]
  (let [created (:created (meta token))
        valid-seconds (:expires_in token)
        expires-at (t/>> (-> created
                             (t/in "America/Los_Angeles"))
                         (t/new-duration valid-seconds :seconds))
        duration (t/duration
                  {:tick/beginning (t/now)
                   :tick/end expires-at})]
    (t/seconds duration)))

(defn access-token!
  []
  (let [refresh-token (:refresh_token @auth-token)
        time-remaining (token-time-remaining @auth-token)

        expired? (< time-remaining 5)

        has-refresh-token? (boolean refresh-token)]

    (when (and expired?
               has-refresh-token?)
      (refresh-token! refresh-token)))

  (:access_token @auth-token))

(defn generate-bearer-token
  []
  (str
   "Bearer "
   (access-token!)))

(defn- xero-code->auth-token-request!
  [code]
  (-> (client/post
       xero-token-endpoint
       {:headers {:authorization (generate-auth-header)}
        :form-params {:grant_type (:authorization-code grant-type-lookup-table)
                      :code code
                      :redirect_uri (-> env :xero :oauth-callback-uri)}
        :as :auto
        :accept :json})
      :body))

(defn xero-code->auth-token!
  [code]
  (reset-auth-token! (xero-code->auth-token-request! code)))

(defn get-items
  []
  (-> (client/get "https://api.xero.com/api.xro/2.0/Items"
                  {:headers {:authorization (generate-bearer-token)
                             :Xero-Tenant-Id (-> env :xero :tenant-id)}
                   :accept :json})
      :body
      (parse-string true)
      :Items))

(defn find-item-by-xero-id
  [xero-id]
  (try+ (-> (client/get (str "https://api.xero.com/api.xro/2.0/Items" "/" xero-id)
                        {:headers {:authorization (generate-bearer-token)
                                   :Xero-Tenant-Id (-> env :xero :tenant-id)}
                         :accept :json})
            :body
            (parse-string true)
            :Items)
        (catch [:status 404] {} nil)))

(defn find-item-by-code
  [code]
  (try+
   (-> (client/get (str "https://api.xero.com/api.xro/2.0/Items" "/" code)
                   {:headers {:authorization (generate-bearer-token)
                              :Xero-Tenant-Id (-> env :xero :tenant-id)}
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
                                                         "AccountCode" "500"})
                  is_sold (assoc "SalesDetails" {"UnitPrice" default_price
                                                 "AccountCode" "400"}))]
    (generate-string payload)))

(defn sync-local<-remote!
  [{:keys [local remote]}])


(defn update-item!
  "Update an item in xero based on local item data"
  [local-item-data xero-item-id]
  (let [opts {:headers {:authorization (generate-bearer-token)
                        :Xero-Tenant-Id (-> env :xero :tenant-id)}
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
  (let [opts {:headers {:authorization (generate-bearer-token)
                        :Xero-Tenant-Id (-> env :xero :tenant-id)}
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

(comment
  (pprint (first (get-items)))

  (pprint (get-items))

  (refresh-token! (:refresh_token @auth-token))

  (token-time-remaining @auth-token)

  ;; 
  )

#_(tap> (pm/logs))