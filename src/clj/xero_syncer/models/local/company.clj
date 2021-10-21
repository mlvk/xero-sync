(ns xero-syncer.models.local.company
  (:require [xero-syncer.db.core :as db]
            [xero-syncer.models.local.generic-record :as gr]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+]]
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

(defn- get-ready-to-sync-company-ids-sql
  [& {:keys [limit]
      :or {limit 200}}]
  (-> (hh/select [:c.id :id])
      (hh/from [:companies :c])
      (hh/where [:and
                 [:= :c.sync_state 0]])
      (hh/limit limit)))

(defn get-ready-to-sync-company-ids [& {:keys [limit]
                                        :or {limit 200}}] (:map :id (db/execute! (#'get-ready-to-sync-company-ids-sql :limit limit))))


(defn remote->local!
  "Sync a xero contact to local company"
  [{:keys [origin-id remote-data]}]
  (let [xero-id (:ContactID remote-data)
        company-name (:Name remote-data)
        maybe-local-record (or
                            (get-company-by-name company-name)
                            (gr/get-record-by-id :companies origin-id))

        change-set {:xero_id xero-id
                    :name (:Name remote-data)}]

    (gr/merge-remote-response->local :orders maybe-local-record remote-data change-set)))