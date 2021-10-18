(ns xero-syncer.services.scheduler
  (:require [chime.core :as chime]
            [mount.core :as mount]
            [nano-id.core :refer [nano-id]]
            [tick.core :as t]))

(declare stop-all-schedules)
#_(defonce schedules (atom []))

(mount/defstate ^{:on-reload :noop} schedules
  :start (atom [])
  :stop (stop-all-schedules))

(defn time-from-now
  "A time from now in duration
   
   Args:

   duration - A duration to offset now from

   Example:

   (time-from-now (t/new-duration 5 :seconds))

   "
  [duration]
  (t/>> (-> (t/now)
            (t/in "America/Los_Angeles"))
        duration))

(defn create-schedule
  "Create a schedule that calls a handler
   
   Required keys: 
   
   :name - Name for this schedule
   :handler - Handler fn to be called at the schedule
   :frequency - Duration between calls

   Optional keys:

   :start-at - An inst to start the schedule
   
   Example: 
   
   (create-schedule 
     :handler (fn [time] (tap> time))
     :frequency (t/new-duration 10 :seconds)
     :start-at (time-from-now (t/new-duration 5 :seconds)))

   "
  [& {:keys [name handler frequency start-at]
      :or {start-at (t/now)}}]
  (let [schedule-id (nano-id)
        schedule (chime/chime-at
                  (chime/periodic-seq
                   start-at
                   frequency)
                  (fn [_] (handler)))]
    (swap! schedules conj {:id schedule-id
                           :name name
                           :frequency frequency
                           :started-at start-at
                           :closeable schedule})))

(defn stop-schedule
  "Stop a running schedule based on it's id"
  [schedule-id]
  (let [matched-schedule (first (filter
                                 (fn [{:keys [id]}] (= id schedule-id))
                                 @schedules))]
    (when matched-schedule
      (let [closeable (:closeable matched-schedule)
            updated-schedules (filter (fn [schedule]
                                        (not (= schedule matched-schedule)))
                                      @schedules)]
        (.close closeable)
        (reset! schedules updated-schedules)))))

(defn stop-all-schedules
  "Stop all running schedules"
  []
  (doseq [schedule @schedules]
    (stop-schedule (:id schedule))))


(comment
  (create-schedule
   :handler (fn [time] (tap> time))
   :frequency (t/new-duration 1 :seconds)
   :start-at (time-from-now (t/new-duration 1 :seconds)))

  @schedules

  (stop-all-schedules)
  (stop-schedule "lp5__ebnbiTLWGzXS2pb6")

  (tap> @schedules)

;;   
  )