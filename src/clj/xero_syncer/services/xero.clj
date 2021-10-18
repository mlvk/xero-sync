(ns xero-syncer.services.xero
  (:require [clj-http.client :as client]
            [clojure.pprint :refer [pprint]]
            [slingshot.slingshot :refer [throw+ try+]]
            [cheshire.core :refer [generate-string parse-string]]
            [ring.util.http-response :refer :all]
            [clojure.tools.logging :as log]
            [postmortem.core :as pm]
            [tick.core :as t]
            [xero-syncer.config :refer [env]])
  (:import java.util.Base64))

(defonce ^:private access-data (atom nil))
(def xero-token-endpoint "https://identity.xero.com/connect/token")
(def grant-type-lookup-table
  {:authorization-code "authorization_code"
   :refresh "refresh_token"})

(defn- encodeB64 [to-encode]
  (.encodeToString (Base64/getEncoder) (.getBytes to-encode)))

(defn- persist-access-data!
  "Persist the provided auth token to local state"
  [data]
  (reset! access-data (with-meta data {:created (t/now)})))

(defn- generate-basic-auth-header
  "Generate the auth header per this spec https://developer.xero.com/documentation/guides/oauth2/auth-flow/#3-exchange-the-code
   Returns - 'Basic client-id:client-secret'"
  []
  (let [client-id (-> env :xero :client-id)
        client-secret (-> env :xero :client-secret)
        key (str client-id ":" client-secret)
        encoded-key (encodeB64 key)]
    (str "Basic " encoded-key)))

(defn has-access-data?
  []
  (boolean (:access_token @access-data)))

(defn refresh-token
  []
  (:refresh_token @access-data))

(defn has-refresh-token?
  []
  (boolean (refresh-token)))

(defn refresh-access-data!
  "Refresh access data"
  []
  (if (has-refresh-token?)
    (let [res (-> (client/post
                   xero-token-endpoint
                   {:headers {:authorization (generate-basic-auth-header)}
                    :form-params {:grant_type (:refresh grant-type-lookup-table)
                                  :refresh_token (refresh-token)}
                    :as :auto
                    :accept :json})
                  :body)]
      (persist-access-data! res)
      (log/info {:what "Access token refresh"
                 :msg "Access token was refreshed successfully"}))
    (log/warn {:what "Missing access-data"
               :msg "No access data found. User must first log in through xero."})))

(defn access-token-time-remaining
  "What is the remaining time in seconds for the provided access token"
  [token]
  (if token
    (let [created (:created (meta token))
          valid-seconds (:expires_in token)
          expires-at (t/>> (-> created
                               (t/in "America/Los_Angeles"))
                           (t/new-duration valid-seconds :seconds))
          duration (t/duration
                    {:tick/beginning (t/now)
                     :tick/end expires-at})]
      (t/seconds duration))
    0))

(defn get-access-token!
  "Returns a valid access-token or nil if expired and missing refresh token.

   This will force a refresh-token call if expired.
   It blocks until the new token arrives."
  []
  (if (has-access-data?)
    (let [refresh-token (refresh-token)
          time-remaining (access-token-time-remaining @access-data)

          expired? (< time-remaining 5)

          has-refresh-token? (boolean refresh-token)]

      (if expired?
        (if has-refresh-token?

          (do
            (refresh-access-data!)
            (:access_token @access-data))

          nil)

        (:access_token @access-data)))
    nil))

(defn generate-bearer-auth-header [] (str "Bearer " (get-access-token!)))

(defn xero-code->access-data!
  "Converts a code to a token per: https://developer.xero.com/documentation/guides/oauth2/auth-flow/#3-exchange-the-code"
  [code]
  (let [res (-> (client/post
                 xero-token-endpoint
                 {:headers {:authorization (generate-basic-auth-header)}
                  :form-params {:grant_type (:authorization-code grant-type-lookup-table)
                                :code code
                                :redirect_uri (-> env :xero :oauth-callback-uri)}
                  :as :auto
                  :accept :json})
                :body)]

    (persist-access-data! res)))

(defn get-items
  []
  (-> (client/get "https://api.xero.com/api.xro/2.0/Items"
                  {:headers {:authorization (generate-bearer-auth-header)
                             :Xero-Tenant-Id (-> env :xero :tenant-id)}
                   :accept :json})
      :body
      (parse-string true)
      :Items))

(comment
  (pprint (first (get-items)))

  (pprint (get-items))

  (refresh-access-data!)

  (access-token-time-remaining @access-data)

  (refresh-access-data!)

  (tap> @access-data)
  ;; 
  )

#_(tap> (pm/logs))