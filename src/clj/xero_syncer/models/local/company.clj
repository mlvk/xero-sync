(ns xero-syncer.models.local.company
  (:require [xero-syncer.db.core :as db]
            [honey.sql :as hs]
            [honey.sql.helpers :as hh]))

(defn- get-company-by-order-id-sql
  [id]
  (-> (hh/select
       [:c.*])
      (hh/from [:orders :o])

      (hh/join [:locations :l] [:= :l.id :o.location_id])
      (hh/join [:companies :c] [:= :c.id :l.company_id])
      (hh/where [:= :o.id id])))

(defn get-company-by-order-id [id] (db/execute-one! (#'get-company-by-order-id-sql id)))

(defn- get-company-by-name-sql
  [name]
  (-> (hh/select :*)
      (hh/from [:companies :c])
      (hh/where [:= :c.name name])))

(defn get-company-by-name [name] (db/execute-one! (#'get-company-by-name-sql name)))