(ns xero-syncer.models.local.location
  (:require [honey.sql.helpers :as hh]
            [xero-syncer.db.core :as db]))

(defn- get-location-by-order-id-sql
  [id]
  (-> (hh/select
       [:l.*])
      (hh/from [:locations :l])
      (hh/join [:orders :o] [:= :l.id :o.location_id])
      (hh/where [:= :o.id id])))

(defn get-location-by-order-id [id] (db/execute-one! (#'get-location-by-order-id-sql id)))