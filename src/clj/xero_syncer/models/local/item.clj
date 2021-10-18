(ns xero-syncer.models.local.item
  (:require [xero-syncer.db.core :as db]
            [honey.sql :as hs]
            [honey.sql.helpers :as hh]))

(defn- get-items-sql
  []
  (-> (hh/select :*)
      (hh/from :items)))

(defn get-items [] (db/execute! (get-items-sql)))

(defn- get-sell-items-sql
  []
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.is_sold true])))

(defn get-sell-items [] (db/execute! (get-sell-items-sql)))

(defn- get-purchased-items-sql
  []
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.is_purchased true])))

(defn get-purchased-items [] (db/execute! (get-purchased-items-sql)))

(defn- get-item-by-id-sql
  [id]
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.id id])))

(defn get-item-by-id [id] (db/execute-one! (get-item-by-id-sql id)))

(defn- get-item-by-xero-id-sql
  [xero-id]
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.xero_id xero-id])))

(defn get-item-by-xero-id [xero-id] (db/execute-one! (get-item-by-xero-id-sql xero-id)))

(defn- get-item-by-code-sql
  [code]
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.code code])))

(defn get-item-by-code [code] (db/execute-one! (get-item-by-code-sql code)))

(defn- get-synced-items-sql
  []
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.sync_state 1])))

(defn get-synced-items [] (db/execute! (get-synced-items-sql)))

(defn- get-unsynced-items-sql
  []
  (-> (hh/select :*)
      (hh/from [:items :i])
      (hh/where [:= :i.sync_state 0])))

(defn get-unsynced-items [] (db/execute! (get-unsynced-items-sql)))

(defn update-item-sql
  [id props]
  (let [fields (reduce (fn [acc [k v]]
                         (when v
                           (into acc {k v})))
                       {}
                       (map identity props))]
    (-> (hh/update [:items :i])
        (hh/where [:= :i.id id])
        (hh/set fields))))

(defn update-item!
  "Update an item based on props. Will only update non nil values
   
   Example: 

   (update-item! 1 {:name 'Hi there'}) => Generates and executes: ['UPDATE items i SET i.name = ? WHERE i.id = ?' 'Hi there' 1]

   "
  [id props] (db/execute-one! (update-item-sql id props)))

(update-item-sql 1 {:name "Hi there"})
;; => {:update [:items :i], :where [:= :i.id 1], :set {:i.name "Hi there"}}


(defn- mark-item-synced-sql
  [id]
  (-> (hh/update [:items :i])
      (hh/set {:sync_state 1})
      (hh/where [:= :i.id id])))

(defn mark-item-synced! [id] (db/execute-one! (mark-item-synced-sql id)))

(defn- mark-item-unsynced-sql
  [id]
  (-> (hh/update [:items :i])
      (hh/set {:sync_state 0})
      (hh/where [:= :i.id id])))

(defn mark-item-unsynced! [id] (db/execute-one! (mark-item-unsynced-sql id)))