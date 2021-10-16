(ns xero-syncer.services.syncer
  (:require [xero-syncer.services.xero :as xero]
            [xero-syncer.db.core :as db]))

(defn sync-items
  []
  (let [remote-items (xero/get-items)
        local-items (db/get-items)]

    {:remote-items remote-items
     :local-items local-items}))





#_(-> (first (db/get-synced-items))
      :id)

#_(db/mark-unsynced! {:id 143})


#_(db/mark-synced! {:id 143})

(defn yoson
  []
  (let [local-item (first (db/get-unsynced-items))
        remote-item (or (xero/find-item-by-xero-id (:xero_id local-item))
                        (xero/find-item-by-code (:xero_id local-item)))
        has-remote? (boolean remote-item)]

    (if has-remote?
      (xero/sync-local<-remote! {:local local-item
                                 :remote remote-item})
      (xero/sync-local->remote! local-item))))

#_(yoson)