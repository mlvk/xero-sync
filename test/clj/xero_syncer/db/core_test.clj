(ns xero-syncer.db.core-test
  (:require [clojure.test :refer :all]
            [java-time.pre-java8]
            [mount.core :as mount]
            [xero-syncer.config :refer [env]]
            [xero-syncer.db.core :as db]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'xero-syncer.config/env
     #'xero-syncer.db.core/conn)

    (f)))