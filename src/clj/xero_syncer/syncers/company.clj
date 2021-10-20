(ns xero-syncer.syncers.company
  (:require [clojure.tools.logging :as log]
            [xero-syncer.constants.topics :as topics]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.local.company :as lc]
            [xero-syncer.models.remote.contact :as rcm]
            [xero-syncer.services.rabbit-mq :as mq]))

(defn remote->local!
  "Sync a xero contact to local company"
  [{:keys [origin-id remote-data]}]

  (let [xero-id (:ContactID remote-data)
        company-name (:Name remote-data)
        local-record (or
                      (gr/get-record-by-xero-id :companies xero-id)
                      (lc/get-company-by-name company-name)
                      (gr/get-record-by-id :companies origin-id))
        local-record-id (:id local-record)
        has-local-record? (boolean local-record)
        change-set {:xero_id xero-id
                    :name (:Name remote-data)}]

    (when has-local-record?
      (gr/update-record! :companies local-record-id change-set))))

(defn local->remote!
  "Sync a local company to xero as a contact"
  [{:keys [data]}]
  (let [local-record-id (:id data)
        result (rcm/sync-local->remote! data)]

    (remote->local! {:origin-id local-record-id
                     :remote-data result})

    (gr/mark-record-synced! :companies local-record-id)

    (log/info {:what "Sync status"
               :direction :local->remote
               :msg (str "Successfully synced company with id: " local-record-id)})))

(defn batch-local->remote
  [{:keys [data]}]
  (let [companies (gr/get-record-by-ids :companies (:ids data))
        results (rcm/upsert-contacts! companies)]

    (doseq [r results]
      (let [match-local (remote->local! {:remote-data r})]
        (tap> {:match-local match-local})
        (when match-local
          (gr/mark-record-synced! :companies (:id match-local))
          (log/info {:what "Sync status"
                     :direction :local->remote
                     :msg (str "Successfully synced company with id: " (:id match-local))}))))))

(defn check-unsynced-local-companies
  "Check for unsynced local company. Pushes results to rabbit mq local->remote queue"
  []
  (let [next-record (first (gr/get-unsynced-records :companies))]
    (when next-record
      (mq/publish :topic topics/sync-local-company :payload {:type :company
                                                             :data next-record}))))

#_(batch-local->remote {:data {:ids (map #(:id %) (gr/get-records :companies))}})

#_(mq/publish :topic topics/sync-local-company :payload {:type :company
                                                         :data (lc/get-company-by-order-id 1764)})

#_(mq/publish :topic topics/sync-local-company :payload {:type :company
                                                         :data (gr/get-record-by-xero-id :companies "4f37ab5f-8ee0-4d54-bf79-57501ba3c90e")})

