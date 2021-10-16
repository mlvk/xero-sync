(ns xero-syncer.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [xero-syncer.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[xero-syncer started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[xero-syncer has shut down successfully]=-"))
   :middleware wrap-dev})
