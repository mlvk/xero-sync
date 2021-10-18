(ns xero-syncer.syncers.item
  (:require [xero-syncer.services.rabbit-mq :as mq]
            [xero-syncer.constants.topics :as topics]
            [xero-syncer.models.remote.item :as rim]
            [xero-syncer.models.local.item :as lim]))

(defn remote->local
  [{:keys [origin-item-id data]}]

  (let [xero-code (:Code data)
        xero-id (:ItemID data)
        local-item (or
                    (lim/get-item-by-id origin-item-id)
                    (lim/get-item-by-code xero-code)
                    (lim/get-item-by-xero-id xero-id))
        local-item-id (:id local-item)
        has-local-item? (boolean local-item)
        update-fields {:code xero-code
                       :xero_id xero-id
                       :name (:Name data)}]

    (when has-local-item?
      (lim/update-item! local-item-id update-fields))))

(defn- handle-xero-item-result
  [origin-item-id data]

  (let [xero-code (:Code data)
        xero-id (:ItemID data)
        local-item (or
                    (lim/get-item-by-id origin-item-id)
                    (lim/get-item-by-code xero-code)
                    (lim/get-item-by-xero-id xero-id))
        local-item-id (:id local-item)
        has-local-item? (boolean local-item)
        update-fields {:code xero-code
                       :xero_id xero-id
                       :name (:Name data)}]

    (when has-local-item?
      (lim/update-item! local-item-id update-fields))))

(defn local->remote
  [{:keys [data]}]
  (let [local-item-id (:id data)
        result (rim/sync-local->remote! data)]

    (handle-xero-item-result local-item-id result)

    (lim/mark-item-synced! (:id data))))

(defn check-unsynced-local-items
  [_]
  (let [next-item (first (lim/get-unsynced-items))]
    (when next-item
      (mq/publish :topic topics/sync-local-item :payload {:type :item
                                                          :data next-item}))))

(comment

  (first (lim/get-sell-items))

  (lim/get-unsynced-items)

  (count (lim/get-unsynced-items))

  (-> (lim/get-items)
      (first)
      (lim/mark-item-unsynced!))

  (first (lim/get-unsynced-items))

  (lim/get-item-by-xero-id {:xero_id "a3265d03-ea44-435a-b2ce-9886d0424890"})


  (lim/get-item-by-code {:code "SUn- O"})

  (-> (lim/get-item-by-xero-id {:xero_id "a3265d03-ea44-435a-b2ce-9886d0424890"})
      (lim/mark-item-unsynced!))

  (-> (lim/get-item-by-id 82)
      :id
      (lim/mark-item-unsynced!))

  (-> (lim/get-item-by-id 82)
      :xero_id)

  ;; 
  )

