(ns xero-syncer.models.remote.contact
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [try+]]
            [xero-syncer.services.xero :as xero]))

(def xero-contacts-endpoint "https://api.xero.com/api.xro/2.0/Contacts/")

(defn local-company->xero-contact-payload
  [{:keys [name
           active_state
           is_vendor
           is_customer]}]

  {"Name" name
   "ContactStatus" (if active_state "ACTIVE" "INACTIVE")
   "IsSupplier" is_vendor
   "IsCustomer" is_customer})

(defn upsert-contacts!
  "Update companies in xero based on local items data"
  [companies]
  (let [body (generate-string {:Contacts (map local-company->xero-contact-payload companies)})
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]
    (try+
     (-> (client/post (str xero-contacts-endpoint "?summarizeErrors=false")
                      payload)
         :body
         (parse-string true)
         :Contacts)
     (catch [:status 403] error (log/error {:what :xero
                                            :msg "Forbidden, cannot access this resource"
                                            :error error}))
     (catch [:status 404] error (log/error {:what :xero
                                            :msg "Couldn't find resource"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Unknown error"
                                            :error error})))))