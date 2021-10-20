(ns xero-syncer.models.remote.contact
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [try+]]
            [xero-syncer.services.xero :as xero]))

(def xero-contacts-endpoint "https://api.xero.com/api.xro/2.0/Contacts/")

(defn find-contact-by-xero-id
  [xero-id]
  (try+ (-> (client/get (str xero-contacts-endpoint (if xero-id xero-id "FORCE-XERO-NIL"))
                        {:headers (xero/generate-auth-headers)
                         :accept :json})
            :body
            (parse-string true)
            :Contacts
            (first))
        (catch [:status 403] error (log/error {:what "Couldn't find contact by xero-id"
                                               :error error}))
        (catch [:status 404] error (log/error {:what "Couldn't find contact by xero-id"
                                               :error error}))))

(defn find-contact-by-name
  [name]
  (try+ (-> (client/get (str xero-contacts-endpoint "?searchTerm=" (if name name "FORCE-XERO-NIL"))
                        {:headers (xero/generate-auth-headers)
                         :accept :json})
            :body
            (parse-string true)
            :Contacts
            (first))
        (catch [:status 403] error (log/error {:what "Couldn't find contact by name"
                                               :error error}))
        (catch [:status 404] error (log/error {:what "Couldn't find contact by name"
                                               :error error}))))

(defn local-company->xero-contact-payload
  [{:keys [name
           active_state
           is_vendor
           is_customer]}]

  {"Name" name
   "ContactStatus" (if active_state "ACTIVE" "INACTIVE")
   "IsSupplier" is_vendor
   "IsCustomer" is_customer})

(defn update-contact!
  "Update a contact in xero based on local company data"
  [local-company-data xero-company-id]
  (let [body (generate-string (local-company->xero-contact-payload local-company-data))
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]

    (try+
     (-> (client/post (str xero-contacts-endpoint xero-company-id)
                      payload)
         :body
         (parse-string true)
         :Contacts
         (first))
     (catch [:status 403] error (log/error {:what "Couldn't update contact"
                                            :error error}))
     (catch [:status 404] res res)
     (catch [:status 400] res res))))

(defn create-contact!
  "Create a new contact in xero based on local company data"
  [local-company-data]
  (let [body (generate-string (local-company->xero-contact-payload local-company-data))
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]

    (try+
     (-> (client/put (str xero-contacts-endpoint)
                     payload)
         :body
         (parse-string true)
         :Contacts
         (first))
     (catch [:status 404] error (log/error {:what "Couldn't create contact"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Couldn't create contact"
                                            :error error})))))

(defn upsert-contacts!
  "Update companies in xero based on local items data"
  [companies]
  (let [body (generate-string {:Contacts (map local-company->xero-contact-payload companies)})
        payload {:headers (xero/generate-auth-headers)
                 :accept :json
                 :body body}]
    (try+
     (-> (client/post xero-contacts-endpoint
                      payload)
         :body
         (parse-string true)
         :Contacts)
     (catch [:status 403] error (log/error {:what "Couldn't update contact"
                                            :error error}))
     (catch [:status 404] error (log/error {:what "Couldn't update contact"
                                            :error error}))
     (catch [:status 400] error (log/error {:what "Couldn't update contact"
                                            :error error})))))

(defn sync-local->remote!
  [local-record-data]

  (let [matched-record (or (find-contact-by-xero-id (:xero_id local-record-data))
                           (find-contact-by-name (:name local-record-data)))
        has-match? (boolean matched-record)
        remote-xero-id (:ContactID matched-record)]

    (if has-match?
      (update-contact! local-record-data remote-xero-id)
      (create-contact! local-record-data))))