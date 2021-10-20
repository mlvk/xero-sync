(ns xero-syncer.models.local.sales-order
  (:require [xero-syncer.db.core :as db]
            [next.jdbc.date-time]
            [honey.sql :as hs]
            [honey.sql.helpers :as hh]))

(defn- get-sales-orders-sql
  []
  (-> (hh/select :*)
      (hh/from [:orders :o])
      (hh/where [:= :o.order_type "sales-order"])))

(defn get-sales-orders [] (db/execute! (get-sales-orders-sql)))

(defn- get-sales-order-number-sql
  [order-number]
  (-> (hh/select :*)
      (hh/from [:orders :o])
      (hh/where [:= :o.order_number order-number])))

(defn get-sales-order-number [order-number] (db/execute-one! (get-sales-order-number-sql order-number)))

(defn- get-sales-order-ids-sql
  []
  (-> (hh/select :o.id)
      (hh/from [:orders :o])
      (hh/where [:= :o.order_type "sales-order"])))

(defn get-sales-order-ids [] (db/execute! (get-sales-order-ids-sql)))

(defn- get-unsynced-published-sales-orders-sql
  []
  (-> (hh/select :*)
      (hh/from [:orders :o])
      (hh/where [:and
                 [:= :o.sync_state 0]
                 [:= :o.published_state 1]])))

(defn get-unsynced-published-sales-orders [] (db/execute! (#'get-unsynced-published-sales-orders-sql)))

(defn- get-order-items-by-order-id-sql
  [id]
  (-> (hh/select
       [:oi.id :order_item_id]
       [:oi.quantity :order_item_quantity]
       [:oi.unit_price :order_item_unit_price]

       [:i.id :item_id]
       [:i.name :item_name]
       [:i.description :item_description]
       [:i.code :item_code])
      (hh/from [:orders :o])
      (hh/join [:order_items :oi] [:= :o.id :oi.order_id])
      (hh/join [:items :i] [:= :i.id :oi.item_id])
      (hh/where [:= :o.id id])))

(defn get-order-items-by-order-id [id] (db/execute! (#'get-order-items-by-order-id-sql id)))

