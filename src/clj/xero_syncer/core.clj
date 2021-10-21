(ns xero-syncer.core
  (:require
   [xero-syncer.handler :as handler]
   [xero-syncer.nrepl :as nrepl]
   [luminus.http-server :as http]
   [luminus-migrations.core :as migrations]
   [xero-syncer.config :refer [env]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.logging :as log]
   [xero-syncer.services.xero]
   [xero-syncer.services.rabbit-mq]
   [xero-syncer.services.syncer]
   [xero-syncer.services.scheduler]
   [mount.core :as mount])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
   (-> env
       (assoc  :handler (handler/app))
       (update :port #(or (-> env :options :port) %))
       (select-keys [:handler :host :port])))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (println "Calling start-app")
  (println args)
  (log/info {:what :service
             :msg "Starting app"})
  (doseq [component (->
                     (mount/except [#'xero-syncer.services.syncer/schedules])
                     (mount/start-with-args #'xero-syncer.config/env)
                     :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (println "Calling main")
  (println args)

  (->
   (mount/except [#'xero-syncer.services.syncer/schedules])
   (mount/start-with-args #'xero-syncer.config/env))

  (cond
    (nil? (-> env :db :host))
    (do
      (log/error "Database configuration not found, :database-url environment variable must be set before running")
      (System/exit 1))
    :else
    (start-app args)))