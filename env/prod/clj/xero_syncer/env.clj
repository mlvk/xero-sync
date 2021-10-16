(ns xero-syncer.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[xero-syncer started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[xero-syncer has shut down successfully]=-"))
   :middleware identity})
