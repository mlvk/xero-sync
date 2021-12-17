(ns xero-syncer.syncers.generic-syncer
  (:require [clojure.tools.logging :as log]
            [xero-syncer.models.local.generic-record :as gr]))

;; @TODO: Change this to a keyed arg list and add the table type and remove the hard coding
(defn merge-back-remote->local!
  "Merge back remote result to local record.
   
   Args

   1. results - The xero result set
   2. update-fn - The function used to find and update the local record for the given result row

   Example: (merge-back-remote->local! results lso/remote->local!)
   "
  #_[results update-fn]
  [& {:keys [table results update-fn]}]
  (doseq [r results]
    (let [match-local (update-fn {:remote-data r})
          id (:id match-local)]
      (if match-local
        ;; @TODO: This should not be hardcoded. This might be the main issue as to why the item is not showing synced state
        (do (gr/mark-record-synced! table id)
            (log/info {:what :sync
                       :direction :remote->local
                       :msg (str "Successfully synced with id: " id)}))
        (log/error {:what :sync
                    :direction :remote->local
                    :remote-data-result r
                    :msg (str "Error syncing. Could not find record with id " id)})))))