(ns syncers.sales-orders
  (:require [xero-syncer.constants.topics :as topics]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.local.order :as lo]
            [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.syncers.sales-order :as sos]))

(comment

  (tap> (->> (gr/get-records :items)
             (filter (fn [r] (= (:sync_state r) 0)))))

;;   ======================
;;   Pure
;;   ======================
  (tap> (first (lo/get-ready-to-sync-sales-orders)))
  (tap> (gr/get-record-by-id :orders 2882))

;;   Count total raedy to be synced
  (count (lo/get-ready-to-sync-sales-orders))

;;   Build the id payload used to batch sync
  {:data {:ids (mapv #(:id %) (vec (take 10 (lo/get-ready-to-sync-sales-orders))))}}

;;   Take some ready to be synces
  (tap> (take 1 (lo/get-ready-to-sync-sales-orders)))

  (tap> (gr/get-record-by-id :orders 48096))

  (tap> (gr/get-record-by-xero-id :orders "bb1c9d8a-bffa-4909-a489-1e66eb269ba5"))

  (tap> {:data {:ids (mapv (fn [i]
                             {:id (:id i)
                              :type (:order_type i)
                              :num (:order_number i)}) (vec (take 100 (lo/get-ready-to-sync-sales-orders))))}})


;;   ======================
;;   Side effecting
;;   ======================

;;   Will run the batch check popping the first item and trying to sync
  (sos/queue-fulfilled-ready-to-sync-sales-orders)

;;   Publish a specific sales order to the sync queue by id
  (mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                             :data (gr/get-record-by-id :orders 2882)})

;;   Batch sync orders
  (sos/batch-local->remote! {:data {:ids (mapv #(:id %) (vec (take 100 (lo/get-ready-to-sync-sales-orders))))}})

;; Batch sync a single id
  (sos/batch-local->remote! {:data {:ids [48096]}}))

