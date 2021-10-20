(ns xero-syncer.syncers.sales-orders
  (:require [clojure.tools.logging :as log]
            [xero-syncer.constants.topics :as topics]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.local.sales-order :as lso]
            [xero-syncer.models.remote.invoice :as ri]
            [xero-syncer.services.rabbit-mq :as mq]))


(defn batch-local->remote!
  "Batch sync sales order to xero invoices. 
   Requires the follow data arg

   Arg - {:data {:ids [1 2 3]}}
   The order ids to sync
   "
  [{:keys [data]}]
  (let [sales-orders (gr/get-record-by-ids :orders (:ids data))
        results (ri/upsert-invoices! sales-orders)]

    (doseq [r results]
      (let [match-local (lso/remote->local! {:remote-data r})]
        (when match-local
          (gr/mark-record-synced! :orders (:id match-local))
          (log/info {:what "Sync status"
                     :direction :local->remote
                     :msg (str "Successfully synced sales order with id: " (:id match-local))}))))))


(defn queue-ready-to-sync-sales-orders
  "Check for unsynced local company. Pushes results to rabbit mq local->remote queue
   
   Args

   Optional
   1. chunk-size - Int - How many invoices to grab at once
   "
  [& {:keys [chunk-size]
      :or {chunk-size 50}}]
  (let [ready-to-sync-sales-order-ids (lso/get-ready-to-sync-sales-orders-ids :limit chunk-size)]
    (when (seq ready-to-sync-sales-order-ids)
      (mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                                 :data {:ids ready-to-sync-sales-order-ids}}))))


#_(gr/get-record-by-ids :orders (lso/get-ready-to-sync-sales-orders-ids :limit 10))