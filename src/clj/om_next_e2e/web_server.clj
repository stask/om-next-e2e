(ns om-next-e2e.web-server
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging      :as log]
   [catacumba.core             :as ct]
   [catacumba.http             :as http]
   [catacumba.handlers.parse   :as parse]
   [catacumba.serializers      :as ser]
   [om.next.server             :as om]
   [clojure.core.async         :as async :refer [>! <! go-loop go]]))

;; ===========================================================================
;; utils

(def app-state (atom {:count 0}))

(def updates-ch (async/chan))
(def updates-mult (async/mult updates-ch))

(defn dispatch [_ key _] key)

(defmulti readf dispatch)

(defmulti mutatef dispatch)

(def parser (om/parser {:read readf :mutate mutatef}))

(defn get-state-key [{:keys [state]} key]
  (let [st @state]
    (if-let [[_ value] (find st key)]
      {:value value}
      :not-found)))

(defmethod readf :default [env key params]
  :not-found)

(defmethod readf :count [env key _]
  (get-state-key env key))

(defmethod mutatef 'ui/increment [{:keys [state]} _ {:keys [value]}]
  {:value {:keys [:count]}
   :action (fn []
             (swap! state update :count (fn [old] (+ old value))))})

(defmethod mutatef :default [_ key params]
  {:error "Not Found"
   :key key
   :params params})

;; ===========================================================================
;; handlers

(defn add-state [& {:as state}]
  (fn [_]
    (ct/delegate state)))

(defn status-handler [_]
  (http/ok "ok"))

(defn query-handler [{:keys [data]}]

  (let [query-env {:state app-state}
        params    data
        _         (log/info "query" params)
        r         (parser query-env params)]
    (log/info "<<" r)
    (go
      (>! updates-ch r)
      (http/ok (ser/encode r :transit+json)
               {"Content-Type" "application/transit+json"}))))

(defn updates-handler [context]
  (ct/sse context
          (fn [_ out]
            (let [c (async/chan)]
              (async/tap updates-mult c)
              (go-loop []
                (when-let [tx (<! c)]
                  (>! out (ser/bytes->str (ser/encode tx :transit+json)))
                  (recur)))))))

;; ===========================================================================
;; component

(defrecord WebServer [port]
  component/Lifecycle
  (start [component]
    (log/info ";; starting WebServer")
    (let [routes [[:assets "" {:dir     "public"
                               :indexes ["index.html"]}]
                  [:any (add-state)]
                  [:get "status" status-handler]
                  [:prefix "api"
                   [:any (parse/body-params)]
                   [:post "1/query" query-handler]
                   [:get "1/updates" updates-handler]]]]
      (assoc component
             :server (ct/run-server (ct/routes routes)
                                    {:port        port
                                     :debug       true
                                     :marker-file "catacumba.basedir"}))))
  (stop [component]
    (log/info ";; stopping WebServer")
    (when-let [server (:server component)]
      (.stop server))
    (dissoc component :server)))

;; ===========================================================================
;; constructor

(defn new-web-server [config]
  (component/using
   (map->WebServer (select-keys config [:port]))
   []))
