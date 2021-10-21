(defproject xero-syncer "0.1.0-SNAPSHOT"

  :description "Xero Syncer"
  :url ""

  :dependencies [[ch.qos.logback/logback-classic "1.2.5"]
                 [clojure.java-time "0.3.3"]
                 [tick "0.5.0-RC2"]
                 [conman "0.9.1"]
                 [cprop "0.1.19"]
                 [expound "0.8.9"]
                 [clj-http "3.12.3"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 [luminus-jetty "0.2.0"]
                 [postmortem "0.5.0"]
                 [luminus-migrations "0.7.1"]
                 [luminus-transit "0.1.2"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.6"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.15"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.16"]
                 [org.clojure/data.codec "0.1.1"]
                 [nrepl "0.8.3"]
                 [com.github.seancorfield/honeysql "2.1.818"]
                 [hikari-cp "2.13.0"]
                 [com.github.seancorfield/next.jdbc "1.2.737"]
                 [com.novemberain/langohr "5.1.0"]
                 [com.taoensso/nippy "3.1.1"]
                 [cheshire "5.10.0"]
                 [crouton "0.1.2"]
                 [org.clojure/tools.reader "1.3.6"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.postgresql/postgresql "42.2.23"]
                 [org.webjars.npm/bulma "0.9.2"]
                 [org.webjars.npm/material-icons "1.0.0"]
                 [org.webjars/webjars-locator "0.41"]
                 [jarohen/chime "0.3.3"]
                 [nano-id "1.0.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-defaults "0.3.3"]
                 [selmer "1.12.44"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main ^:skip-aot xero-syncer.core

  :plugins []

  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "xero-syncer.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :reveal {:dependencies [[vlaaad/reveal "1.3.224"]]
            :repl-options {:nrepl-middleware [vlaaad.reveal.nrepl/middleware]}}

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn"]
                  :dependencies [[org.clojure/tools.namespace "1.1.0"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [ring/ring-devel "1.9.4"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]
                                 [cider/cider-nrepl "0.26.0"]]

                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
