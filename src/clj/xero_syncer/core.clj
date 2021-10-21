(ns xero-syncer.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [luminus.http-server :as http]
            [mount.core :as mount]
            [xero-syncer.config :refer [env]]
            [xero-syncer.handler :as handler]
            [xero-syncer.nrepl :as nrepl]
            [xero-syncer.services.rabbit-mq]
            [xero-syncer.services.scheduler]
            [xero-syncer.services.syncer]
            [xero-syncer.services.xero]
            [xero-syncer.specs.env :as env-validator])
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
  (let [parsed-args (parse-opts args cli-options)
        mounted-components (-> (mount/with-args parsed-args)
                               (mount/except [#'xero-syncer.services.syncer/schedules])
                               (mount/start)
                               :started)]
    (doseq [component mounted-components]
      (log/info component "started")))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (-> args
      (parse-opts cli-options)
      (mount/start-with-args #'xero-syncer.config/env))

  (if (env-validator/is-valid? env)
    (start-app args)
    (do
      (log/error {:what :core
                  :msg (env-validator/explain env)})
      (System/exit 1))))

