(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [cprop.core :refer [load-config]]
            [cprop.tools :as t]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [postmortem.core :as pm]
            [xero-syncer.core]
            [xero-syncer.config]
            [clojure.tools.logging :as log]
            [xero-syncer.db.core :as db]
            [xero-syncer.services.rabbit-mq]
            [xero-syncer.services.syncer]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (println "Starting")
  (mount/start-without #'xero-syncer.services.syncer/schedules
                       #'xero-syncer.core/repl-server))

(defn stop
  "Stops application."
  []
  (log/info {:what :core
             :msg "Stop"})
  (mount/stop-except #'xero-syncer.core/repl-server))

(defn restart
  "Restarts application."
  []
  (log/info {:what :core
             :msg "Restart"})
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

(defn start-rabbit-mq-subscriptions
  "Start rabbitmq subscriptions"
  []
  (mount/start #'xero-syncer.services.syncer/subscriptions))

(defn stop-rabbit-mq-subscriptions
  "Stops rabbitmq subscriptions"
  []
  (mount/stop #'xero-syncer.services.syncer/subscriptions))

(defn restart-rabbit-mq-subscriptions
  "Restarts rabbitmq subscriptions"
  []
  (mount/stop #'xero-syncer.services.syncer/subscriptions)
  (mount/start #'xero-syncer.services.syncer/subscriptions))

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

(defn reload-config
  "Reload config"
  []
  (mount/stop #'xero-syncer.config/env)
  (mount/start #'xero-syncer.config/env))

(defn reset-pm
  []
  (pm/reset!))

(defn print-env-vars
  []
  (print (slurp (t/map->env-file
                 (load-config)))))
