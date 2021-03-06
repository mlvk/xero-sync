(ns xero-syncer.syncers.item
  (:require [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.constants.topics :as topics]
            [postmortem.core :as pm]
            [xero-syncer.models.remote.item :as ri]
            [xero-syncer.syncers.generic-syncer :as gs]
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
    (gs/merge-back-remote->local!
     :table :items
     :results results
     :update-fn li/remote->local!)))

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
  (log/info {:what :sync
             :msg "Starting force sync all items"})
  (let [all-active-items (gr/get-records :items)
        ids (map :id all-active-items)]

    (mq/publish :topic topics/sync-local-item :payload {:type :item
                                                        :data {:ids ids}})))

(defn force-sync-items
  [ids]
  (log/info {:what :sync
             :msg "Starting force sync items"})
  (mq/publish :topic topics/sync-local-item :payload {:type :item
                                                      :data {:ids ids}}))


#_(mq/publish :topic topics/sync-local-item :payload {:type :item
                                                      :data {:ids [(li/get-item-by-code "r-016")]}})

#_(count (gr/get-records :items :where [:= :t.active true]))


#_(force-sync-all-items)

#_(gr/get-records :items :where [:= :t.active true])

#_(li/get-ready-to-sync-item-ids :limit 10)
;; => (398)

#_(ri/upsert-items! (gr/get-record-by-ids :items [398]))
#_(queue-ready-to-sync-items)
  ;; => [{:SalesDetails {:UnitPrice 0.0, :AccountCode "400", :TaxType "OUTPUT"}, :ItemID "65f134e0-9bdf-4e29-bbec-afdba143db55", :UpdatedDateUTC "/Date(1639704445249)/", :IsSold true, :IsTrackedAsInventory false, :IsPurchased true, :Code "MLVK-RIS-002", :Name "Sun-dried Tomato + Olive Quinoa Risotto", :Description "Creamy Cashew Risotto w/Sun-dried Tomatoes + Kalamata Olives", :ValidationErrors [], :PurchaseDescription "Creamy Cashew Risotto w/Sun-dried Tomatoes + Kalamata Olives", :StatusAttributeString "OK", :PurchaseDetails {:UnitPrice 0.0, :AccountCode "500", :TaxType "NONE"}}]

  ;; => Execution error (ExceptionInfo) at slingshot.support/stack-trace (support.clj:201).
  ;;    clj-http: status 401
