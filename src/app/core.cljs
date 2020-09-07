(ns app.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [app.bluetooth :as ble]
            ["react-plotly.js" :default Plot]
            [cljs.core.async :as a :refer [go-loop]]
            [app.msgpack :as msgpack]))

;; Event Management
(defn send-event [event payload]
  (when-let [chan @ble/tx-bytes-chan]
    (a/>! chan (msgpack/encode {:event event :payload payload}))))

(defmulti handle-event (fn [event] (:event event)))

(defmethod handle-event :alert [event]
  (js/alert (:payload event)))

(defn start-event-handler []
  (go-loop []
    (when-let [chan @ble/rx-events-chan]
      (handle-event (a/<! chan))
      (recur))))

#_(defn plot-data []
  [{:x @ble/data
    :nbinsx 128
    :type "histogram"}])

#_(defn plot-layout []
  {:width 600
   :height 600
   :title "Histogram"})

#_(defn plot [data layout]
  [:> Plot {:data (data) :layout (layout)}])

(defn app []
  [:div
   (if @ble/connected?
     [:h1 {:style {:color "blue"}} "BLE Connected: " (str (.-name @ble/ble-device))]
     [:h1 {:style {:color "red"}} "BLE Disconnected"])
   [:div
    (if-not @ble/connected?
      [:input {:type "button"
               :on-click #(do (ble/connect!)
                              (start-event-handler))
               :value "Connect"}]
   [:input {:type "button"
            :on-click #(ble/disconnect!)
            :value "Disconnect"}])
    (when-let [chan @ble/tx-bytes-chan]
      [:input {:type "button"
               :on-click #(send-event :alert "Hello World")
               :value "Send hello world"}])]
   #_[plot plot-data plot-layout]])

(defn ^:dev/after-load start []
  (.log js/console "Starting app")
  (rdom/render [app] (.getElementById js/document "app")))

(defn ^:export main []
  (start))
