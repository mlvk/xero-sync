(ns xero-syncer.syncers.company
  (:require [clojure.tools.logging :as log]
            [xero-syncer.constants.topics :as topics]
            [xero-syncer.models.local.company :as lc]
            [xero-syncer.syncers.generic-syncer :as gs]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.remote.contact :as rc]
            [xero-syncer.services.rabbit-mq :as mq]))

(defn batch-local->remote!
  "Batch sync company to xero contact. 
   Requires the follow data arg

   Arg - {:data {:ids [1 2 3]}}
   The company ids to sync
   "
  [{:keys [data]}]
  (let [companies (gr/get-record-by-ids :companies (:ids data))
        results (rc/upsert-contacts! companies)]

    (gs/merge-back-remote->local!
     :table :companies
     :results results
     :update-fn lc/remote->local!)))

(defn queue-ready-to-sync-companies
  "Check for unsynced local companies Pushes results to rabbit mq local->remote queue
   
   Args

   Optional
   1. chunk-size - Int - How many companies to grab at once
   "
  [& {:keys [chunk-size]
      :or {chunk-size 50}}]
  (let [ready-to-sync-company-ids (lc/get-ready-to-sync-company-ids :limit chunk-size)]
    (when (seq ready-to-sync-company-ids)
      (mq/publish :topic topics/sync-local-company :payload {:type :company
                                                             :data {:ids ready-to-sync-company-ids}}))))


(defn force-sync-all-companies
  []
  (log/info {:what :sync
             :msg "Starting force sync all companies"})
  (let [all-active-companies (gr/get-records :companies)
        ids (map :id all-active-companies)]

    (mq/publish :topic topics/sync-local-company :payload {:type :company
                                                           :data {:ids ids}})))

(defn force-sync-companies
  [ids]
  (log/info {:what :sync
             :msg "Starting force sync companies"})
  (mq/publish :topic topics/sync-local-company :payload {:type :company
                                                         :data {:ids ids}}))

#_(force-sync-all-companies)

#_(batch-local->remote {:data {:ids (map #(:id %) (gr/get-records :companies))}})

#_(mq/publish :topic topics/sync-local-company :payload {:type :company
                                                         :data {:id [(lc/get-company-by-order-id 1764)]}})

#_(mq/publish :topic topics/sync-local-company :payload {:type :company
                                                         :data {:ids [(gr/get-record-by-xero-id :companies "4f37ab5f-8ee0-4d54-bf79-57501ba3c90e")]}})

