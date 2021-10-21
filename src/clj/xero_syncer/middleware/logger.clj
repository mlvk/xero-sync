(ns xero-syncer.middleware.logger
  (:require
   [clojure.walk :as walk]
   [cheshire.core :refer [generate-string]]

   [clojure.tools.logging :as log :refer [info]]))

(defn replace-keys
  "Redacts the values found in m for each key in redact-keys.
  The redacted value is obtained by applying redact-value-fn to key and value"
  [m {:keys [replace-key?]}]
  (walk/postwalk (fn [x]
                   (if (map? x)
                     (->> x
                          (map (fn [[k v]]
                                 (if (replace-key? (keyword k))
                                   [k ((constantly "[REDACTED]") k v)]
                                   [k v])))
                          (into {}))
                     x))
                 m))

(defn redact-private-header-data
  "Redacts private header information"
  [request]
  (let [redacted-headers (replace-keys (:headers request) {:replace-key? #{:x-api-key :authorization :password}})
        redacted-parameters (replace-keys (:parameters request) {:replace-key? #{:x-api-key :authorization :password}})]
    (assoc request
           :headers redacted-headers
           :parameters redacted-parameters)))

(defn wrap-logger-middleware
  "Logger middleware"
  [handler]
  (fn [request]

    (-> (redact-private-header-data request)
        (select-keys [:headers :parameters])
        (generate-string)
        (info))
    (handler request)))