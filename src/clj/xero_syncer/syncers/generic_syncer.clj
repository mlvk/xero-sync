(ns xero-syncer.syncers.generic-syncer
  (:require [clojure.tools.logging :as log]
            [xero-syncer.models.local.generic-record :as gr]))

(defn merge-back-remote->local!
  "Merge back remote result to local record.
   
   Args

   1. results - The xero result set
   2. update-fn - The function used to find and update the local record for the given result row

   Example: (merge-back-remote->local! results lso/remote->local!)
   "
  [results update-fn]
  (doseq [r results]
    (let [match-local (update-fn {:remote-data r})
          id (:id match-local)]
      (if match-local
        (do (gr/mark-record-synced! :orders id)
            (log/info {:what :sync
                       :direction :remote->local
                       :msg (str "Successfully synced with id: " id)}))
        (log/error {:what :sync
                    :direction :remote->local
                    :remote-data-result r
                    :msg (str "Error syncing. Could not find record with id " id)})))))