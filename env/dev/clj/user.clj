(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [xero-syncer.core]
            [clojure.pprint :refer [pprint]]
            [postmortem.core :as pm]
            [xero-syncer.db.core :as db]
            [xero-syncer.services.rabbit-mq]
            [cprop.core :refer [load-config]]
            [cprop.tools :as t]
            [xero-syncer.services.syncer]))

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
  (mount/stop #'xero-syncer.db.core/conn)
  (mount/start #'xero-syncer.db.core/conn))

(defn restart-rabbit-mq
  "Restarts rabbitmq connections"
  []
  (mount/stop #'xero-syncer.services.rabbit-mq/chan)
  (mount/stop #'xero-syncer.services.rabbit-mq/conn)

  (mount/start #'xero-syncer.services.rabbit-mq/conn)
  (mount/start #'xero-syncer.services.rabbit-mq/chan))

(defn stop-rabbit-mq
  "Restarts rabbitmq connections"
  []
  (mount/stop #'xero-syncer.services.rabbit-mq/chan)
  (mount/stop #'xero-syncer.services.rabbit-mq/conn))

(defn start-rabbit-mq
  "Restarts rabbitmq connections"
  []
  (mount/start #'xero-syncer.services.rabbit-mq/conn)
  (mount/start #'xero-syncer.services.rabbit-mq/chan))

(defn restart-schedules
  "Restarts rabbitmq connections"
  []
  (mount/stop #'xero-syncer.services.syncer/schedules)
  (mount/start #'xero-syncer.services.syncer/schedules))

(defn stop-schedules
  "Restarts rabbitmq connections"
  []
  (mount/stop #'xero-syncer.services.syncer/schedules))

(defn start-schedules
  "Restarts rabbitmq connections"
  []
  (mount/start #'xero-syncer.services.syncer/schedules))

(defn reset-pm
  []
  (pm/reset!))

(defn print-env-vars
  []
  (print (slurp (t/map->env-file
                 (load-config)))))
