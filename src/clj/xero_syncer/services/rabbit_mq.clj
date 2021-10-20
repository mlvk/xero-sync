(ns xero-syncer.services.rabbit-mq
  (:require [clojure.edn]
            [langohr.basic     :as lb]
            [langohr.channel   :as lch]
            [langohr.consumers :as lc]
            [langohr.core      :as rmq]
            [langohr.exchange :as le]
            [langohr.queue     :as lq]
            [mount.core :as mount]
            [slingshot.slingshot :refer [try+]]
            [time-literals.read-write]
            [xero-syncer.config :refer [env]]))

(def main-exchange "main-exchange")
(def local->remote-queue "local->remote")

(declare start)

(defonce consumers (atom []))

(mount/defstate ^{:on-reload :noop} conn
  :start (rmq/connect {:uri (:cloudamqp-url env)})
  :stop (rmq/close conn))

(mount/defstate ^{:on-reload :noop} chan
  :start (lch/open conn)
  :stop (lch/close chan))

(mount/defstate ^{:on-reload :noop} mq-data
  :start (start))

(defn- edn-response-decoder
  [payload]
  (clojure.edn/read-string
   {:readers time-literals.read-write/tags}
   (String. payload "UTF-8")))

(defn unsubscribe!
  [tag-id]
  (lb/cancel chan tag-id))

(defn unsubscribe-tags!
  [& {:keys [tags]
      :or {tags @consumers}}]
  (doseq [c tags]
    (try+ (unsubscribe! c)
          (catch Object _ "Invalid tag"))
    (reset! consumers (filter #(not (= c %)) @consumers))))

(defn publish
  [& {:keys [exchange topic payload]
      :or {exchange main-exchange}}]
  (lb/publish chan exchange topic (prn-str payload "UTF-8")))

(defn subscribe
  "Subscribe to a rabbit mq topic"
  [queue handler & {:keys [opts]
                    :or {opts {:auto-ack true}}}]
  (let [tag-id (lc/subscribe chan queue (fn [ch meta payload]
                                          {:payload payload}
                                          (handler ch meta (edn-response-decoder payload))) opts)]
    (swap! consumers conj tag-id) tag-id))

(defn create-default-exchanges
  [exchanges]
  (doseq [{:keys [name type]} exchanges]
    (le/declare chan name type)))

(defn create-queues
  [queues]
  (doseq [queue queues]
    (lq/declare chan queue)))

(defn create-routes
  [routes]
  (doseq [{:keys [queue exchange route-key]} routes]
    (lq/bind chan queue exchange {:routing-key route-key})))

(defn start
  []
  (create-default-exchanges [{:name main-exchange
                              :type "topic"}])

  (create-queues [local->remote-queue])

  (create-routes [{:exchange main-exchange
                   :queue local->remote-queue
                   :route-key "sync.local.*"}]))

(comment

  (reset! consumers [])

  (subscribe "local->remote" (fn [ch meta res] (tap> {:subscribed-as "local->remote"
                                                      :res res})))

  (publish :topic "sync.local.item" :payload {:name "Carrot2"})

;;   
  )
