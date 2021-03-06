(ns com.rootedinsights.slacktastic.listener.main
  (:require [com.rootedinsights.slacktastic.lib
                                   [slack-rtm :as rtm]
                                   [es :as es]]
            [com.rootedinsights.slacktastic.config.es :as es-config]

            [cheshire.core :as json]
            [clojure.java.io :as jio]))

(defn get-config
  []
  (-> (jio/resource "config.json")
       slurp
       (json/parse-string true)))

(defn- message-handler
  [& [args]]
  (println args))

(defn- get-proper-response
  [response]
  (-> (json/parse-string response true)
      (dissoc :ts)
      (assoc :timestamp (System/currentTimeMillis))))

(defn setup-elastic
  []
  (let [config (get-config)
        es-conn (es/connect-es config)]
    (if 
      (es/ensure-index! 
        es-conn (:index_name config) es-config/settings es-config/mappings)
      {:conn es-conn 
       :index-name (:index_name config)
       :mapping (:mapping config)}
      nil)))

(defn start
  [options]
  (let [rtm-conn (rtm/start options)
        options (setup-elastic)]
    (rtm/subscribe "message" 
        #(es/elastic-feed (assoc options :response (get-proper-response %))))
    rtm-conn))
