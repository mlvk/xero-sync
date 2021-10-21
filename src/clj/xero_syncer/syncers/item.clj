(ns xero-syncer.syncers.item
  (:require [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.constants.topics :as topics]
            [postmortem.core :as pm]
            [xero-syncer.models.remote.item :as ri]
            [clojure.tools.logging :as log]
            [xero-syncer.models.local.generic-record :as gr]
            [xero-syncer.models.local.item :as li]))

(defn batch-local->remote!
  "Batch sync items to xero items. 
   Requires the follow data arg

   Arg - {:data {:ids [1 2 3]}}
   The item ids to sync
   "
  [{:keys [data]}]
  (let [items (gr/get-record-by-ids :items (:ids data))
        results (ri/upsert-items! items)]

    (doseq [r results]
      (let [match-local (li/remote->local! {:remote-data r})]
        (when match-local
          (gr/mark-record-synced! :items (:id match-local))
          (log/info {:what "Sync status"
                     :direction :local->remote
                     :msg (str "Successfully synced item with id: " (:id match-local))}))))))

(defn queue-ready-to-sync-items
  "Check for unsynced local item Pushes results to rabbit mq local->remote queue
   
   Args

   Optional
   1. chunk-size - Int - How many items to grab at once
   "
  [& {:keys [chunk-size]
      :or {chunk-size 50}}]
  (let [ready-to-sync-item-ids (li/get-ready-to-sync-item-ids :limit chunk-size)]
    (when (seq ready-to-sync-item-ids)
      (mq/publish :topic topics/sync-local-item :payload {:type :item
                                                          :data {:ids ready-to-sync-item-ids}}))))

(defn force-sync-all-items
  []
  (log/info {:what "Sync"
             :msg "Starting force sync all items"})
  (let [all-active-items (gr/get-records :items :where [:= :t.active true])
        ids (map :id all-active-items)]

    (mq/publish :topic topics/sync-local-item :payload {:type :item
                                                        :data {:ids ids}})))



#_(mq/publish :topic topics/sync-local-item :payload {:type :item
                                                      :data {:ids [(li/get-item-by-code "r-016")]}})

#_(count (gr/get-records :items :where [:= :t.active true]))


#_(force-sync-all-items)

#_(gr/get-records :items :where [:= :t.active true])