(ns om-next-e2e.system
  (:require
   [com.stuartsierra.component :as component]
   [om-next-e2e.web-server     :refer [new-web-server]]))

(defn new-system [config]
  (component/system-map
   :web-server (new-web-server (:web-server config))))
