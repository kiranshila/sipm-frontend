(ns app.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [app.bluetooth :as ble]
            ["react-plotly.js" :default Plot]))

(def data (r/atom []))

(defn plot-data []
  [{:x (map first @data)
                 :y (map second @data)
                 :type "scatter"
                 :mode "lines+markers"
                 :marker {:color "red"}}])

(defn plot-layout []
  {:width 600
                  :height 600
                  :title "A Fancy Plot"})

(defn plot [data layout]
  [:> Plot {:data (data) :layout (layout)}])

(defn app []
  [:div
   (if @ble/connected?
     [:h1 {:style {:color "blue"}} "BLE Connected: " (str (.-name @ble/ble-device))]
     [:h1 {:style {:color "red"}} "BLE Disconnected"])
   [:input {:type "button"
            :on-click #(ble/connect!)
            :value "Connect"}]
   [:br]
   [:br]
   [:br]
   [:input {:type "button"
            :on-click #(ble/disconnect!)
            :value "Disconnect"}]
   [:br]
   [:br]
   [:br]
   [plot plot-data plot-layout]])

(defn ^:dev/after-load start []
  (.log js/console "Starting app")
  (rdom/render [app] (.getElementById js/document "app")))

(defn ^:export main []
  (start))
