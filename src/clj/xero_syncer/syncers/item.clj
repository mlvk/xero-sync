(ns xero-syncer.syncers.item
  (:require [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.constants.topics :as topics]
            [xero-syncer.models.remote.item :as rim]
            [clojure.tools.logging :as log]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.local.item :as lim]))

(defn remote->local
  "Sync a local item to xero"
  [{:keys [origin-item-id data]}]

  (let [xero-code (:Code data)
        xero-id (:ItemID data)
        local-item (or
                    (gr/get-record-by-id :items origin-item-id)
                    (lim/get-item-by-code xero-code)
                    (gr/get-record-by-xero-id :items xero-id))
        local-item-id (:id local-item)
        has-local-item? (boolean local-item)
        update-fields {:code xero-code
                       :xero_id xero-id
                       :name (:Name data)}]

    (when has-local-item?
      (gr/update-record! :items local-item-id update-fields))))

(defn- handle-xero-item-result!
  "Handle the result of a xero sync and merge back changes locally"
  [data]
  (let [xero-code (:Code data)
        xero-id (:ItemID data)
        local-item (or
                    (lim/get-item-by-code xero-code)
                    (gr/get-record-by-xero-id :items xero-id))
        local-item-id (:id local-item)
        has-local-item? (boolean local-item)
        update-fields {:code xero-code
                       :xero_id xero-id
                       :name (:Name data)}]

    (when has-local-item?
      (gr/update-record! :items local-item-id update-fields))))

(defn local->remote
  [{:keys [data]}]
  (let [local-item-id (:id data)
        result (rim/sync-local->remote! data)]

    (handle-xero-item-result! result)
    (gr/mark-record-synced! :items local-item-id)

    (log/info {:what "Sync status"
               :direction :local->remote
               :msg (str "Successfully synced item with id: " local-item-id)})))

(defn batch-local->remote
  [{:keys [data]}]
  (let [items (gr/get-record-by-ids :items (:ids data))
        results (rim/upsert-items! items)]

    (doseq [r results]
      (let [match-local (handle-xero-item-result! r)]
        (when match-local
          (gr/mark-record-synced! :items (:id match-local))
          (log/info {:what "Sync status"
                     :direction :local->remote
                     :msg (str "Successfully synced item with id: " (:id match-local))}))))))

(defn check-unsynced-local-items
  "Check for unsynced local items. Pushes results to rabbit mq local->remote queue"
  []
  (let [next-item (first (gr/get-unsynced-records :items))]
    (when next-item
      (mq/publish :topic topics/sync-local-item :payload {:type :item
                                                          :data next-item}))))

#_(mq/publish :topic topics/sync-local-item :payload {:type :item
                                                      :data (lim/get-item-by-code "r-016")})