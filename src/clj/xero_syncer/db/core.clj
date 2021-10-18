(ns xero-syncer.db.core
  (:require [clojure.edn :as edn]
            [clojure.string]
            [clojure.tools.logging :as log]
            [xero-syncer.config :refer [env]]
            [hikari-cp.core :refer [close-datasource make-datasource]]
            [honey.sql :as sql]
            [honey.sql.helpers :as hh]
            [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time]
            [next.jdbc.prepare]
            [next.jdbc.result-set :as rs]))

(defstate conn
  :start (do
           (log/info "Connected to host" (-> env :db :host))
           (make-datasource {:pool-name          "db-pool"
                             :adapter            "postgresql"
                             :username           (-> env :db :user)
                             :password           (-> env :db :password)
                             :database-name      (-> env :db :name)
                             :server-name        (-> env :db :host)
                             :port-number        (-> env :db :port)
                             :register-mbeans    false}))
  :stop (close-datasource conn))

(defn execute-raw
  [sql & opts]
  (jdbc/execute! conn sql
                 (merge {:return-keys true
                         :builder-fn rs/as-unqualified-lower-maps
                         :pretty true} opts)))

(defn execute!
  [q & {:keys [connection opts]
        :or {connection conn
             opts {}}}]
  (jdbc/execute! connection (sql/format q)
                 (merge {:return-keys true
                         :builder-fn rs/as-unqualified-lower-maps
                         :pretty true} opts)))

(defn execute-one!
  [q & {:keys [connection opts]
        :or {connection conn
             opts {}}}]
  (jdbc/execute-one! connection (sql/format q)
                     (merge {:return-keys true
                             :builder-fn rs/as-unqualified-lower-maps
                             :pretty true} opts)))

(defn insert-row!
  [table value]
  (->
   (hh/insert-into table)
   (hh/values [value])
   execute-one!))

(defn insert-rows!
  [table values]
  (->
   (hh/insert-into table)
   (hh/values values)
   execute!))

(defn execute-in-transaction!
  [statements]
  (jdbc/with-transaction [tx conn]
    (doseq [statement statements]
      (execute! statement :connection tx))))

(defn raw-columns->prepared-columns
  [columns]
  (cond
    (vector? columns) (mapv (fn [column] [(keyword column)]) columns)
    (string? columns) (keyword columns)
    :else :*))

(defn raw-params->prepared-order-by
  [params]
  (when
   (string? params) (->> (clojure.string/split params #":")
                         (mapv keyword))))

(defn total-pages
  [total-rows per-page]
  (-> (/ total-rows per-page)
      Math/ceil
      int
      dec
      (max 0)))

(def max-results (fnil min 100 100))

(defn get-rows!
  "Get rows from table. Can pass in options pred, columns, order
   Refer to honeysql docs for option formatting.

   For order clause
   '?order=updated_at' - ASC is default

   For DESC
   '?order=updated_at:desc'

   For predicate - Must be URL encoded. Refer to honey sql clauses
   '?pred=[:or [:= :id 1] [:= :id 2]]
   "
  [table & {:keys [columns
                   pred
                   order
                   debug]}]
  (let [columns (raw-columns->prepared-columns columns)

        order (raw-params->prepared-order-by order)

        pred (if (string? pred)
               (edn/read-string pred)
               pred)

        query (-> (cond-> {:select columns}
                    true (hh/from table)
                    pred (hh/where pred)
                    order (hh/order-by order)))]
    (if debug
      (sql/format query)
      (execute! query))))

(defn get-rows-paginated!
  "Get rows from table. Can pass in options pred, columns, page, per-page, order
   Refer to honeysql docs for option formatting.

   Max results are 100

   Page & per-page query params
   '?page=0&per-page=100'

   To use multiple columns filters add query params as follows:
   '?columns=id&columns=given_name'

   For order clause
   '?order=updated_at' - ASC is default

   For DESC
   '?order=updated_at:desc'

   For predicate - Must be URL encoded. Refer to honey sql clauses
   '?pred=[:or [:= :id 1] [:= :id 2]]
   "
  [table & {:keys [columns
                   page
                   per-page
                   pred
                   order
                   debug]}]

  (let [columns (raw-columns->prepared-columns columns)

        order (raw-params->prepared-order-by order)

        page (or page 0)

        pred (if (string? pred)
               (edn/read-string pred)
               pred)

        per-page (max-results per-page 100)

        total-rows (-> (cond-> (hh/select :%count.*)
                         true (hh/from table)
                         pred (hh/where pred))
                       execute-one!
                       :count)

        total-pages (total-pages total-rows per-page)

        offset (* page per-page)

        has-more? (< page total-pages)

        query (-> (cond-> {:select columns}
                    true (hh/from table)
                    true (hh/limit per-page)
                    true (hh/offset offset)

                    pred (hh/where pred)
                    order (hh/order-by order)))]

    (if debug
      {:results (sql/format query)
       :total-pages total-pages
       :has-more has-more?
       :page page
       :per-page per-page}
      {:results (execute! query)
       :total-pages total-pages
       :has-more has-more?
       :page page
       :per-page per-page})))