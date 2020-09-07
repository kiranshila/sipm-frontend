(ns app.msgpack
  (:require
   ["@msgpack/msgpack" :refer [Encoder Decoder]]
   [cljs-bean.core :refer [->clj ->js]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [cljs.core.async :as a]))

;; Set up msgpack
(def encoder (Encoder.))
(def decoder (Decoder.))

;; Utility functions for encoding and decoding msgpack
(defn encode [obj]
  (.encode ^js encoder (->js obj)))

(defn decode [^js bytes]
  (->clj (.decode ^js decoder bytes)))

#_(defn decode-async [chan]
  (a/go
    (a/go-loop [decoded? false
              bytes (<! chan)]
      (when decoded?
        (throw (.createNoExtraBytesError decoder (.-totalPost decoder))))
      (.appendBuffer decoder bytes)
      (try (let [object (.doDecodeSync decoder)
                 decoded? true]
             #_(process))))))

(defn decode-async [^js buffer]
  (->clj (<p! (.decodeAsync ^js decoder buffer))))
