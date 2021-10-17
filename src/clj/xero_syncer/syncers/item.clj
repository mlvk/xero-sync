(ns xero-syncer.syncers.item
  (:require [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.constants.topics :as topics]
            [xero-syncer.services.xero :as xero]
            [xero-syncer.db.core :as db]))

(defn ^{:status :pending} remote->local
  [{:keys [type data]}]

  (db/update-item!)

  (tap> {:type :remote
         :data data}))

(defn local->remote
  [{:keys [data]}]
  (let [result (xero/sync-local->remote! data)]
    (remote->local {:type :result
                    :data result})
    (db/mark-item-synced! {:id (:id data)})))

(defn check-unsynced-local-items
  [_]
  (let [next-item (first (db/get-unsynced-items))]
    (when next-item
      (mq/publish :topic topics/sync-local-item :payload {:type :item
                                                          :data next-item}))))

(comment

  (first (db/get-sell-items))

  (db/get-unsynced-items)

  (count (db/get-unsynced-items))

  (-> (db/get-items)
      (first)
      (db/mark-item-unsynced!))

  (first (db/get-unsynced-items))

  (db/get-item-by-xero-id {:xero_id "a3265d03-ea44-435a-b2ce-9886d0424890"})


  (db/get-item-by-code {:code "SUn- O"})

  (-> (db/get-item-by-xero-id {:xero_id "a3265d03-ea44-435a-b2ce-9886d0424890"})
      (db/mark-item-unsynced!))

  (-> (db/get-item-by-id {:id 87})
      (db/mark-item-unsynced!))

  (first (db/get-items))

  ;; 
  )

(first (db/get-sell-items))