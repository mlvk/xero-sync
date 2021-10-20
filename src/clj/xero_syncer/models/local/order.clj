(ns xero-syncer.models.local.order
  (:require [xero-syncer.db.core :as db]
            [next.jdbc.date-time]
            [honey.sql :as hs]
            [xero-syncer.models.local.generic-record :as gr]
            [honey.sql.helpers :as hh]))

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

(defn- get-order-by-order-number-sql
  [order-number]
  (-> (hh/select :*)
      (hh/from [:orders :o])
      (hh/where [:= :o.order_number order-number])))

(defn get-order-by-order-number [order-number] (db/execute-one! (get-order-by-order-number-sql order-number)))

(defn- get-order-items-by-order-number-sql
  [order-number]
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
      (hh/where [:= :o.order_number order-number])))

(defn get-order-items-by-order-number [order-number] (db/execute! (#'get-order-items-by-order-number-sql order-number)))
