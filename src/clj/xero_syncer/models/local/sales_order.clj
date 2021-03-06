(ns xero-syncer.models.local.sales-order
  (:require [xero-syncer.db.core :as db]
            [slingshot.slingshot :refer [try+]]

            [clojure.tools.logging :as log]
            [next.jdbc.date-time]
            [honey.sql :as hs]

            [xero-syncer.models.local.order :as lo]
            [xero-syncer.models.local.generic-record :as gr]
            [honey.sql.helpers :as hh]))

(defn- get-sales-orders-sql
  []
  (-> (hh/select :*)
      (hh/from [:orders :o])
      (hh/where [:= :o.order_type "sales-order"])))

(defn get-sales-orders [] (db/execute! (get-sales-orders-sql)))

(defn- get-sales-order-ids-sql
  []
  (-> (hh/select :o.id)
      (hh/from [:orders :o])
      (hh/where [:= :o.order_type "sales-order"])))

(defn get-sales-order-ids [] (db/execute! (get-sales-order-ids-sql)))

(defn- get-ready-to-sync-sales-orders-sql
  []
  (-> (hh/select [:o.id :order_id]
                 :o.*
                 [[:count :oi.id] :total_line_items])
      (hh/from [:orders :o])
      (hh/left-join [:order_items :oi] [:= :o.id :oi.order_id])
      (hh/join [:fulfillments :f] [:= :o.id :f.order_id])
      (hh/where [:and
                 [:= :o.sync_state 0]
                 [:= :o.order_type "sales-order"]
                 [:= :o.published_state 1]
                 [:= :f.delivery_state 1]])
      (hh/group-by :o.id)
      (hh/having [:> [:count :oi.id] 0])))

(defn get-ready-to-sync-sales-orders [] (db/execute! (#'get-ready-to-sync-sales-orders-sql)))

(defn- get-fulfilled-ready-to-sync-sales-orders-ids-sql
  [& {:keys [limit]
      :or {limit 200}}]
  (-> (hh/select [:o.id :id])
      (hh/from [:orders :o])
      (hh/left-join [:order_items :oi] [:= :o.id :oi.order_id])
      (hh/join [:fulfillments :f] [:= :o.id :f.order_id])
      (hh/where [:and
                 [:= :o.sync_state 0]
                 [:= :o.order_type "sales-order"]
                 [:= :o.published_state 1]
                 [:= :f.delivery_state 1]])
      (hh/group-by :o.id)
      (hh/having [:> [:count :oi.id] 0])
      (hh/limit limit)))

(defn get-fulfilled-ready-to-sync-sales-orders-ids [& {:keys [limit]
                                                       :or {limit 200}}] (map :id (db/execute! (#'get-fulfilled-ready-to-sync-sales-orders-ids-sql :limit limit))))

(defn- get-unfulfilled-ready-to-sync-sales-orders-ids-sql
  [& {:keys [limit]
      :or {limit 200}}]
  (-> (hh/select [:o.id :id])
      (hh/from [:orders :o])
      (hh/left-join [:order_items :oi] [:= :o.id :oi.order_id])
      (hh/join [:fulfillments :f] [:= :o.id :f.order_id])
      (hh/where [:and
                 [:= :o.sync_state 0]
                 [:= :o.order_type "sales-order"]
                 [:= :o.published_state 1]
                 [:= :f.delivery_state 0]
                 [:< :o.delivery_date [:raw ["now() - interval '" 1 " day'"]]]])
      (hh/group-by :o.id)
      (hh/having [:> [:count :oi.id] 0])
      (hh/limit limit)))

(defn get-unfulfilled-ready-to-sync-sales-orders-ids [& {:keys [limit]
                                                         :or {limit 200}}] (map :id (db/execute! (#'get-unfulfilled-ready-to-sync-sales-orders-ids-sql :limit limit))))

(defn remote->local!
  "Sync a xero invoice order to order"
  [{:keys [origin-id remote-data]}]

  (let [xero-id (:InvoiceID remote-data)
        order-number (:InvoiceNumber remote-data)
        change-set {:xero_id xero-id
                    :order_number order-number}

        maybe-local-record (or
                            (lo/get-order-by-order-number order-number)
                            (gr/get-record-by-id :orders origin-id))]

    (gr/merge-remote-response->local :orders maybe-local-record remote-data change-set)))