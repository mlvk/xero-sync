(ns xero-syncer.models.local.item
  (:require [xero-syncer.db.core :as db]
            [xero-syncer.models.local.generic-record :as gr]
            [honey.sql :as hs]
            [honey.sql.helpers :as hh]))

(defn- get-item-by-code-sql
  [code]
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.code code])))

(defn get-item-by-code [code] (db/execute-one! (get-item-by-code-sql code)))

(defn- get-item-by-name-sql
  [name]
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.name name])))

(defn get-item-by-name [name] (db/execute-one! (get-item-by-name-sql name)))

(defn- get-sell-items-sql
  [& {:keys [select]
      :or {select :*}}]
  (-> (hh/select select)
      (hh/from [:items :i])
      (hh/where [:= :i.is_sold true])))

(defn get-sell-items [& {:keys [select]
                         :or {select :*}}] (db/execute! (get-sell-items-sql :select select)))

(defn- get-ready-to-sync-item-ids-sql
  [& {:keys [limit]
      :or {limit 200}}]
  (-> (hh/select [:i.id :id])
      (hh/from [:items :i])
      (hh/where [:and
                 [:= :i.sync_state 0]
                 [:= :i.active true]])
      (hh/limit limit)))

(defn get-ready-to-sync-item-ids [& {:keys [limit]
                                     :or {limit 200}}] (map :id (db/execute! (#'get-ready-to-sync-item-ids-sql :limit limit))))

(defn remote->local!
  "Handle the result of a xero sync and merge back changes locally"
  [{:keys [origin-id remote-data]}]
  (let [xero-code (:Code remote-data)
        xero-name (:Name remote-data)
        xero-id (:ItemID remote-data)
        local-item (or
                    (get-item-by-code xero-code)
                    (get-item-by-name xero-name)
                    (gr/get-record-by-xero-id :items xero-id)
                    (gr/get-record-by-id :companies origin-id))
        local-item-id (:id local-item)
        has-local-item? (boolean local-item)
        update-fields {:code xero-code
                       :xero_id xero-id
                       :name xero-name}]

    (tap> {:remote-data remote-data
           :local-item-id local-item-id
           :update-fields update-fields})

    (when has-local-item?
      (gr/update-record! :items local-item-id update-fields))))