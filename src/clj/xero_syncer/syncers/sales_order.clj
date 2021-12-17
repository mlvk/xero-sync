(ns xero-syncer.syncers.sales-order
  (:require [clojure.tools.logging :as log]
            [xero-syncer.constants.topics :as topics]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.local.sales-order :as lso]
            [xero-syncer.syncers.generic-syncer :as gs]
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

    (gs/merge-back-remote->local!
     :table :orders
     :results results
     :update-fn lso/remote->local!)))

(defn queue-fulfilled-ready-to-sync-sales-orders
  "Check for unsynced local sales order Pushes results to rabbit mq local->remote queue
   
   Args

   Optional
   1. chunk-size - Int - How many invoices to grab at once
   "
  [& {:keys [chunk-size]
      :or {chunk-size 50}}]
  (let [ready-to-sync-sales-order-ids (lso/get-fulfilled-ready-to-sync-sales-orders-ids :limit chunk-size)]
    (when (seq ready-to-sync-sales-order-ids)
      (mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                                 :data {:ids ready-to-sync-sales-order-ids}}))))

(defn queue-unfulfilled-ready-to-sync-sales-orders
  "Check for unsynced local sales order Pushes results to rabbit mq local->remote queue
   
   Args

   Optional
   1. chunk-size - Int - How many invoices to grab at once
   "
  [& {:keys [chunk-size]
      :or {chunk-size 50}}]
  (let [unfulfilled-ready-to-sync-sales-order-ids (lso/get-unfulfilled-ready-to-sync-sales-orders-ids :limit chunk-size)]
    (when (seq unfulfilled-ready-to-sync-sales-order-ids)
      (mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                                 :data {:ids unfulfilled-ready-to-sync-sales-order-ids}}))))

(defn force-sync-sales-orders
  [ids]
  (log/info {:what :sync
             :msg "Starting force sync sales-orders"})
  (mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                             :data {:ids ids}}))

#_(gr/get-record-by-ids :orders (lso/get-ready-to-sync-sales-orders-ids :limit 10))

#_(queue-unfulfilled-ready-to-sync-sales-orders)

#_(lso/get-fulfilled-ready-to-sync-sales-orders-ids)
#_(lso/get-unfulfilled-ready-to-sync-sales-orders-ids)

#_(mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                             :data {:ids [48320]}})