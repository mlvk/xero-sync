(ns xero-syncer.syncers.sales-orders
  (:require [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.constants.topics :as topics]
            [clojure.tools.logging :as log]
            [honey.sql :as hs]
            [xero-syncer.models.local.sales-order :as lso]
            [xero-syncer.models.remote.invoice :as ri]
            [xero-syncer.models.local.generic-record :as gr]))

(defn remote->local!
  "Sync a xero invoice to order company"
  [{:keys [origin-id remote-data]}]

  (let [xero-id (:InvoiceID remote-data)
        order-number (:InvoiceNumber remote-data)
        local-record (or
                      (gr/get-record-by-id :orders origin-id)
                      (lso/get-sales-order-number order-number)
                      (gr/get-record-by-xero-id :orders xero-id))
        local-record-id (:id local-record)
        has-local-record? (boolean local-record)
        change-set {:xero_id xero-id
                    :order_number (:InvoiceNumber remote-data)}]

    (when has-local-record?
      (gr/update-record! :orders local-record-id change-set))))

(defn local->remote!
  "Sync a local order to xero as an invoice"
  [{:keys [data]}]
  (let [local-record-id (:id data)
        result (ri/sync-local->remote! data)
        has-result? (boolean result)]

    (if
     has-result?
      (do (remote->local! {:origin-id local-record-id
                           :remote-data result})

          (gr/mark-record-synced! :orders local-record-id)

          (log/info {:what "Sync status"
                     :direction :local->remote
                     :msg (str "Successfully synced order with id: " local-record-id)}))

      (log/error {:what "Sync status"
                  :direction :local->remote
                  :msg (str "There was a problem syncing sales order: " local-record-id)}))))

(defn batch-local->remote
  [{:keys [data]}]
  (let [sales-orders (gr/get-record-by-ids :orders (:ids data))
        results (ri/upsert-invoices! sales-orders)]

    (doseq [r results]
      (let [match-local (remote->local! {:remote-data r})]
        (tap> {:match-local match-local})
        (when match-local
          (gr/mark-record-synced! :orders (:id match-local))
          (log/info {:what "Sync status"
                     :direction :local->remote
                     :msg (str "Successfully synced sales order with id: " (:id match-local))}))))))



(defn check-unsynced-local-sales-orders
  "Check for unsynced local company. Pushes results to rabbit mq local->remote queue"
  []
  (let [next-record (first (lso/get-unsynced-published-sales-orders))]
    (when next-record
      (mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                                 :data next-record}))))

#_(check-unsynced-local-sales-orders)
#_(tap> (first (lso/get-unsynced-published-sales-orders)))

#_(mq/publish :topic topics/sync-local-sales-order :payload {:type :sales-order
                                                             :data (gr/get-record-by-id :orders 2882)})

#_(tap> (gr/get-record-by-id :orders 2882))
#_(count (lso/get-unsynced-published-sales-orders))
;; => 115

;; => 116

;; => 118

;; => 118


#_{:data {:ids (mapv #(:id %) (vec (take 10 (lso/get-unsynced-published-sales-orders))))}}
;; => {:data {:ids [3696 3240 6172 5507 5371 8723 7261 8129 13507 9021]}}

;; => {:data {:ids (3696 3240 6172 5507 5371 8723 7261 8129 13507 9021)}}

;; => {:data {:ids (3767 3477)}}

#_(batch-local->remote {:data {:ids (mapv #(:id %) (vec (take 1 (lso/get-unsynced-published-sales-orders))))}})

#_(take 1 (lso/get-unsynced-published-sales-orders))
;; => ({:delivery_date #time/date "2017-05-10", :internal_note nil, :location_id 108, :updated_at #time/date-time "2017-05-09T23:22:37.539781", :xero_id nil, :submitted_at nil, :id 5507, :comment nil, :published_state 1, :xero_financial_record_state 0, :shipping 0.0M, :order_number "so-170510-d895", :sync_state 0, :order_type "sales-order", :created_at #time/date-time "2017-05-08T20:45:56.643289"})





