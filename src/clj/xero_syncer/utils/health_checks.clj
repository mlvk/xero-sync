(ns xero-syncer.utils.health-checks)

(defn health-check
  [_]
  {:status 200
   :body {:msg "All systems running"}})