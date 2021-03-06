(ns xero-syncer.models.local.generic-record
  (:require [xero-syncer.db.core :as db]
            [honey.sql :as hs]
            [slingshot.slingshot :refer [try+]]

            [clojure.tools.logging :as log]
            [honey.sql.helpers :as hh]))

(defn- get-records-sql
  [table & {:keys [where]
            :or {where []}}]
  (-> (hh/select :*)
      (hh/from [table :t])
      (hh/where where)))

(defn get-records
  "Get all records for a given table"
  [table & {:keys [where]
            :or {where []}}] (db/execute! (#'get-records-sql table :where where)))

(defn- get-record-by-ids-sql
  [table ids]
  (-> (hh/select :*)
      (hh/from [table :t])
      (hh/where [:in :t.id ids])))

(defn get-record-by-ids
  "Get all records for a given table"
  [table ids] (db/execute! (#'get-record-by-ids-sql table ids)))

(defn- get-record-by-id-sql
  [table id]
  (-> (hh/select :*)
      (hh/from [table :t])
      (hh/where [:= :t.id id])))

(defn get-record-by-id
  "Get record by id for given table"
  [table id] (db/execute-one! (#'get-record-by-id-sql table id)))


(defn- get-record-by-xero-id-sql
  [table xero-id]
  (-> (hh/select :*)
      (hh/from [table :t])
      (hh/where [:= :t.xero_id xero-id])))

(defn get-record-by-xero-id
  "Get record by it's xero id for a given table"
  [table xero-id] (db/execute-one! (get-record-by-xero-id-sql table xero-id)))

(defn- get-synced-records-sql
  [table]
  (-> (hh/select :*)
      (hh/from [table :t])
      (hh/where [:= :t.sync_state 1])))

(defn get-synced-records
  "Get synced records for given table"
  [table] (db/execute! (get-synced-records-sql table)))

(defn- get-unsynced-record-sql
  [table]
  (-> (hh/select :*)
      (hh/from [table :t])
      (hh/where [:= :t.sync_state 0])))

(defn get-unsynced-records
  "Get unsynced records for given table"
  [table] (db/execute! (get-unsynced-record-sql table)))

(defn update-record-sync-state-sql
  [table id sync-state]
  (let [sync-state (if sync-state 1 0)]
    (-> (hh/update [table :t])
        (hh/set {:sync_state sync-state})
        (hh/where [:= :t.id id]))))

(defn mark-record-synced!
  "Mark a record synced"
  [table id] (db/execute-one! (update-record-sync-state-sql table id true)))

(defn mark-record-unsynced!
  "Mark a record unsynced"
  [table id] (db/execute-one! (update-record-sync-state-sql table id false)))

(defn update-record-sql
  [table id props]
  (let [fields (reduce (fn [acc [k v]]
                         (when v
                           (into acc {k v})))
                       {}
                       (map identity props))]
    (-> (hh/update [table :t])
        (hh/where [:= :t.id id])
        (hh/set fields))))

(defn update-record!
  "Update a record based on props. Will only update non nil values
   
   Example: 

   (update-record! :items 1 {:name 'Hi there'}) => Generates and executes: ['UPDATE items t SET t.name = ? WHERE t.id = ?' 'Hi there' 1]

   "
  [table id props]
  (db/execute-one! (update-record-sql table id props)))

(defn merge-remote-response->local
  "Attempts to merge remote data to a local match
   
   Args
   1. table - The table to set changes on
   2. local-record - The record that will be updated
   3. remote-data - The remote data used to build the change-set
   4. change-set - The actual data that will attempt to be merge to the local record

   Example: (merge-remote-response->local :orders {:id 1} {'InvoiceID' 'new-xero-id-001'} {:xero_id 'new-xero-id-001'})
   "
  [table local-record remote-data change-set]
  (let [local-record-id (:id local-record)
        has-local-record? (boolean local-record)]
    (if has-local-record?
      (try+
       (update-record! table local-record-id change-set)
       (catch org.postgresql.util.PSQLException pe (log/error {:what :pg-error
                                                               :error (.getServerErrorMessage pe)
                                                               :remote-data remote-data
                                                               :local-record local-record})))
      (log/warn {:what :sync
                 :msg "No local item found, skipping upstream sync"
                 :remote-data remote-data
                 :local-record local-record}))))