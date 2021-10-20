(ns xero-syncer.models.local.generic-record
  (:require [xero-syncer.db.core :as db]
            [honey.sql :as hs]
            [clojure.tools.logging :as log]
            [honey.sql.helpers :as hh]))

(defn- get-records-sql
  [table]
  (-> (hh/select :*)
      (hh/from table)))

(defn get-records
  "Get all records for a given table"
  [table] (db/execute! (#'get-records-sql table)))

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
  (log/info {:what "Update"
             :table table
             :msg (str "Successfully updated record with id: " id)})
  (db/execute-one! (update-record-sql table id props)))



#_(-> (get-records :companies)
      (first))
;; => {:terms 0, :is_vendor false, :name "Pure Pressed LA", :location_code_prefix "pur", :updated_at #inst "2018-10-13T07:30:44.666627000-00:00", :xero_id "ec6bb2a2-9898-4909-bf9c-12fa9b5fe463", :price_tier_id 5, :id 79, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-03-09T23:07:44.538292000-00:00"}


#_(get-record-by-id :items 135)

#_(mark-record-unsynced! :items 135)

#_(get-record-by-id :companies 78)
  ;; => {:terms 0, :is_vendor false, :name "Sara Bordeos", :location_code_prefix "sar", :updated_at #inst "2017-02-23T23:33:38.278559000-00:00", :xero_id "02b0ee55-07d7-48a3-8411-409c7dee1750", :price_tier_id 3, :id 78, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-02-23T23:33:32.938764000-00:00"}

  ;; => {:terms 0, :is_vendor false, :name "Sara Bordeos", :location_code_prefix "sar", :updated_at #inst "2017-02-23T23:33:38.278559000-00:00", :xero_id "fd5f2457-e337-4982-a14a-862c0babb19a", :price_tier_id 3, :id 78, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-02-23T23:33:32.938764000-00:00"}

  ;; => {:terms 0, :is_vendor false, :name "Sara Bordeos", :location_code_prefix "sar", :updated_at #inst "2017-02-23T23:33:38.278559000-00:00", :xero_id "fd5f2457-e337-4982-a14a-862c0babb19a", :price_tier_id 3, :id 78, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-02-23T23:33:32.938764000-00:00"}

  ;; => {:terms 0, :is_vendor false, :name "Sara Bordeos", :location_code_prefix "sar", :updated_at #inst "2017-02-23T23:33:38.278559000-00:00", :xero_id "fd5f2457-e337-4982-a14a-862c0babb19a", :price_tier_id 3, :id 78, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-02-23T23:33:32.938764000-00:00"}

  ;; => {:terms 0, :is_vendor false, :name "Sara Bordeos", :location_code_prefix "sar", :updated_at #inst "2017-02-23T23:33:38.278559000-00:00", :xero_id "fd5f2457-e337-4982-a14a-862c0babb19a", :price_tier_id 3, :id 78, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-02-23T23:33:32.938764000-00:00"}

  ;; => {:terms 0, :is_vendor false, :name "Sara Bordeos", :location_code_prefix "sar", :updated_at #inst "2017-02-23T23:33:38.278559000-00:00", :xero_id "fd5f2457-e337-4982-a14a-862c0babb19a", :price_tier_id 3, :id 78, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-02-23T23:33:32.938764000-00:00"}

  ;; => {:terms 0, :is_vendor false, :name "Pure Pressed LA", :location_code_prefix "pur", :updated_at #inst "2018-10-13T07:30:44.666627000-00:00", :xero_id "ec6bb2a2-9898-4909-bf9c-12fa9b5fe463", :price_tier_id 5, :id 79, :is_customer true, :active_state 0, :sync_state 1, :created_at #inst "2017-03-09T23:07:44.538292000-00:00"}


#_(mark-record-unsynced! :companies 78)
  ;; => {:terms 0, :is_vendor false, :name "Sara Bordeos", :location_code_prefix "sar", :updated_at #inst "2017-02-23T23:33:38.278559000-00:00", :xero_id "fd5f2457-e337-4982-a14a-862c0babb19a", :price_tier_id 3, :id 78, :is_customer true, :active_state 0, :sync_state 0, :created_at #inst "2017-02-23T23:33:32.938764000-00:00"}

  ;; => {:terms 0, :is_vendor false, :name "Pure Pressed LA", :location_code_prefix "pur", :updated_at #inst "2018-10-13T07:30:44.666627000-00:00", :xero_id "ec6bb2a2-9898-4909-bf9c-12fa9b5fe463", :price_tier_id 5, :id 79, :is_customer true, :active_state 0, :sync_state 0, :created_at #inst "2017-03-09T23:07:44.538292000-00:00"}
