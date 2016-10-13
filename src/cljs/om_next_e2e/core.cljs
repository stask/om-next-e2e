(ns om-next-e2e.core
  (:require
   [goog.dom           :as gdom]
   [cljs-http.client   :as http]
   [cljs.core.async    :as async :refer [<! >! put! chan]]
   [om.next            :as om :refer-macros [defui]]
   [om.dom             :as dom]
   [cognitect.transit  :as t]
   [devtools.core      :as devtools]
   [om-next-e2e.config :as config])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")
    (devtools/install! [:formatters :hints])))

(defn transit->clj [x]
  (t/read (t/reader :json) x))

(defn event-source [url]
  (let [source  (new js/EventSource url)
        channel (chan)]
    (.addEventListener source "message"
                       (fn [event]
                         (put! channel
                               (-> (transit->clj (.-data event))
                                   (vary-meta merge {:event-id   (.-lastEventId event)
                                                     :event-type (keyword (.-type event))})))))
    (set! (.-onerror source)
          (fn [error]
            (.close source)
            (async/close! channel)))
    {:event-source source
     :channel      channel}))

(defn send-post [path query cb]
  (let [req (http/post path {:transit-params query})]
    (go (cb (<! req)))))

(defn send-query [query cb]
  (send-post "/api/1/query" query cb))

(defn send-to-api [{:keys [api] :as remotes} cb]
  (send-query api (fn [{:keys [body status]}]
                    (when (= status 200)
                      (cb body)))))

(defonce app-state (atom {:count 0}))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default [{:keys [state ast]} key params]
  (let [st @state]
    (if-let [[_ value] (find st key)]
      {:value value :api ast}
      :not-found)))

(defmethod mutate 'ui/increment [{:keys [state ast]} _ {:keys [value]}]
  {:value {:keys [:count]}
   :remote true
   :api ast})

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})
    :send send-to-api
    :remotes [:api]}))

(defn mutate! [query]
  (om/transact! reconciler query))

(defui Counter
  static om/IQuery
  (query [this]
    [:count])
  Object
  (render [this]
    (dom/div nil
      (dom/div nil (str "Counter: " (:count (om/props this))))
      (dom/button #js {:onClick #(mutate! `[(ui/increment {:value 1}) :count])}
                  "Increment"))))

(defn mount-root []
  (let [{:keys [channel]} (event-source "/api/1/updates")]
    (go-loop []
      (when-let [msg (<! channel)]
        (doseq [{:keys [result]} (vals msg)]
          (om/merge! reconciler result))
        (recur))))
  (om/add-root! reconciler
    Counter (gdom/getElement "app")))

(defn ^:export init []
  (println "init")
  (dev-setup)
  (mount-root))
