(ns app.bluetooth
  (:require
   [cljs.core.async :as a :refer [go go-loop]]
   [reagent.core :as r]
   [cljs.core.async.interop :refer-macros [<p!]]
   [cljs.pprint :refer [char-code]]))

;; Create Bluetooth instance
(def bluetooth (.-bluetooth js/navigator))

;; Packet size in bytes. Nordic supports up to 244, but 20 works on all chipsets
;; This could get bumped up for better performnace
(def MTU 20)

;; GATT UUIDs
(def ble-nus-service "6e400001-b5a3-f393-e0a9-e50e24dcca9e")
(def ble-nus-char-rx "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
(def ble-nus-char-tx "6e400003-b5a3-f393-e0a9-e50e24dcca9e")

;; Channel to put to, to write to the device
;; This gets reset to nil on disconnects
(def tx-chan (r/atom nil))

;; Channel to take from, to read from the device
;; This also gets reset to nil on disconnects
(def rx-chan (r/atom nil))

;; Some state
(def connected? (r/atom nil))
(def ble-device (r/atom nil))

;; Converting to JS byte arrays
(defmulti to-bytes
  (fn [item] (cond (int? item) :int (string? item) :string)))

(defmethod to-bytes :string [item]
  (.from js/Uint8Array (map char-code item)))

(defmethod to-bytes :int [item]
  (->> (.toString item 16)
       (re-seq #".{1,2}")
       (map #(js/parseInt % 16))
       (.from js/Uint8Array)))

(defn on-rx! [^js event]
  (go
    (when-let [chan @rx-chan]
      (>! chan (.. event -target -value))
      (println "got message"))))

(defn on-connect! [device ^js tx-chara ^js rx-chara]
  (reset! ble-device device)
  (reset! tx-chan (a/chan))
  (reset! rx-chan (a/chan))
  (reset! connected? true)
  (.startNotifications tx-chara)
  (.addEventListener tx-chara "characteristicvaluechanged" on-rx!)
  (go-loop []
    (when-let [chan @tx-chan]
      (let [item (<! chan)
            bytes (to-bytes item)]
        (loop [byte-array bytes]
          (let [chunk (.slice byte-array 0 MTU)]
            (<p! (.writeValue rx-chara chunk))
            (when (> (.-length byte-array) MTU)
              (recur (.slice byte-array MTU))))))
      (recur))))

(defn on-disconnect! []
  (reset! ble-device nil)
  (reset! tx-chan nil)
  (reset! rx-chan nil)
  (reset! connected? false))

(defn get-device [service & services]
  (when bluetooth
    (println "Connecting to device")
    (let [device (.requestDevice bluetooth (clj->js {:optionalServices (into [service] services)
                                                     :acceptAllDevices true}))]
      (println "Device connected")
      device)))

(defn get-server [^js device]
  (when device
    (println "Found " (.-name device))
    (println "Connecting to GATT Server...")
    (.addEventListener device "gattserverdisconnected" on-disconnect!)
    (.connect (.-gatt device))))

(defn get-nus-service [^js server]
  (when server
    (println "Locating NUS Service")
    (let [nus-service (.getPrimaryService server ble-nus-service)]
      (println "Found NUS Service: " (.-uuid nus-service))
      nus-service)))

(defn get-characteristic [^js service char-uuid]
  (when service
    (println "Locating Characteristic: " char-uuid)
    (let [characteristic (.getCharacteristic service char-uuid)]
      (println "Found Characteristic!")
      characteristic)))

(defn connect! []
  (when bluetooth
    (go (let [device (<p! (get-device ble-nus-service))
              server (<p! (get-server device))
              nus-service (<p! (get-nus-service server))
              tx-chara (<p! (get-characteristic nus-service ble-nus-char-tx))
              rx-chara (<p! (get-characteristic nus-service ble-nus-char-rx))]
          (on-connect! device tx-chara rx-chara)))))

(defn disconnect! []
  (when-let [^js device @ble-device]
    (when (.. device -gatt -connected)
      (.. device -gatt disconnect)
      (on-disconnect!))))
