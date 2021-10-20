(ns xero-syncer.models.local.item
  (:require [xero-syncer.db.core :as db]
            [honey.sql :as hs]
            [honey.sql.helpers :as hh]))

(defn- get-item-by-code-sql
  [code]
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.code code])))

(defn get-item-by-code [code] (db/execute-one! (get-item-by-code-sql code)))

(defn- get-sell-items-sql
  [& {:keys [select]
      :or {select :*}}]
  (-> (hh/select select)
      (hh/from [:items :i])
      (hh/where [:= :i.is_sold true])))

(defn get-sell-items [& {:keys [select]
                         :or {select :*}}] (db/execute! (get-sell-items-sql :select select)))