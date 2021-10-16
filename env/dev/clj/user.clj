(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [xero-syncer.config :refer [env]]
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [postmortem.core :as pm]
   [mount.core :as mount]
   [xero-syncer.core :refer [start-app]]
   [xero-syncer.db.core :as db]
   [conman.core :as conman]
   [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'xero-syncer.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'xero-syncer.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'xero-syncer.db.core/*db*)
  (mount/start #'xero-syncer.db.core/*db*)
  (binding [*ns* (the-ns 'xero-syncer.db.core)]
    (xero-syncer.db.core/bind-sql-files)
    #_(conman/bind-connection xero-syncer.db.core/*db* "sql/queries.sql")))

(defn reset-db
  "Resets database."
  []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate
  "Migrates database up for all outstanding migrations."
  []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback
  "Rollback latest database migration."
  []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  (migrations/create name (select-keys env [:database-url])))

(defn reset-pm
  []
  (pm/reset!))

