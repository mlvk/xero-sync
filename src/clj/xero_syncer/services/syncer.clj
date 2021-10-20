(ns xero-syncer.services.syncer
  (:require [mount.core :as mount]
            [tick.core :as t]
            [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.services.scheduler :as scheduler]
            [xero-syncer.services.xero :as xero]
            [xero-syncer.syncers.item :as item-syncer]
            [xero-syncer.syncers.company :as company-syncer]
            [xero-syncer.syncers.sales-orders :as sales-order-syncer]))

(declare create-subscriptions! create-schedules!)

(mount/defstate ^{:on-reload :noop} subscriptions
  :start (create-subscriptions!)
  :stop (mq/unsubscribe-tags! :tags subscriptions))

(mount/defstate ^{:on-reload :noop} schedules
  :start (create-schedules!)
  :stop (scheduler/stop-all-schedules))

(defn local->remote-sync-handler
  "Handler for the local->remote-sync queue"
  [_ _ payload]
  (case (:type payload)
    :item (item-syncer/local->remote payload)
    :company (company-syncer/local->remote! payload)
    :sales-order (sales-order-syncer/local->remote! payload)))

;; @TODO - WIP - Need to decide if we want upstream sync
#_(defn remote->local-sync-handler
    "Handler for the remote->local-sync queue"
    [_ _ payload]
    (case (:type payload)
      :item (item-syncer/remote->local payload)
      :company (company-syncer/remote->local! payload)
      :sales-order (sales-order-syncer/remote->local! payload)))

(defn create-subscriptions!
  "Create all rabbit mq subscriptions. Subscribe to new messages on a queue"
  []
  [(mq/subscribe mq/local->remote-queue #'local->remote-sync-handler)

  ;;  Disable upstream sync for now
   #_(mq/subscribe mq/remote->local-queue #'remote->local-sync-handler)])

(defn create-schedules!
  "Create all schedules. Functions to be called on a schedule"
  []
  [(scheduler/create-schedule
    :name "Check for unsynced local items"
    :handler #'item-syncer/check-unsynced-local-items
    :frequency (t/new-duration 10 :seconds))

   (scheduler/create-schedule
    :name "Check for unsynced local companies"
    :handler #'company-syncer/check-unsynced-local-companies
    :frequency (t/new-duration 10 :seconds))

   (scheduler/create-schedule
    :name "Check for unsynced sales orders"
    :handler #'sales-order-syncer/check-unsynced-local-sales-orders
    :frequency (t/new-duration 10 :seconds))

   (scheduler/create-schedule
    :name "Refresh access data"
    :handler #'xero/refresh-access-data!
    :frequency (t/new-duration 15 :minutes))])

subscriptions

(comment

  (create-subscriptions!)

  (mq/unsubscribe-tags!)

  (mq/publish :topic "sync.local.item" :payload {:type :item
                                                 :name "carrots"})
  (mq/publish :topic "sync.remote.item" :payload {:type :item
                                                  :name "carrots xero"})

;;   
  )


