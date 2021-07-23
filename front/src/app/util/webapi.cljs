;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.util.webapi
  (:require
   [lambdaisland.uri :as u]
   [app.util.exceptions :as ex]
   [app.util.object :as obj]
   [cuerdas.core :as str]
   [goog.dom :as dom]))

(defn classnames
  [& params]
  (assert (even? (count params)))
  (str/join " " (reduce (fn [acc [k v]]
                          (if (true? (boolean v))
                            (conj acc (name k))
                            acc))
                        []
                        (partition 2 params))))


(defn get-element
  [id]
  (dom/getElement id))

(defn create-element
  ([n] (create-element name {}))
  ([n attrs]
   (let [element (.createElement js/document n)]
     (doseq [[k v] attrs]
       (let [k (if (keyword? k) (name k) k)]
         (unchecked-set element k v)))
     element)))

(defn get-context
  [node n]
  (.getContext ^js node n))

(defn stop-propagation!
  [e]
  (when e
    (.stopPropagation e)))

(defn prevent-default!
  [e]
  (when e
    (.preventDefault e)))

(defn get-target
  "Extract the target from event instance."
  [event]
  (.-target event))

(defn get-value
  "Extract the value from dom node."
  [node]
  (.-value node))

(defn checked?
  "Check if the node that represents a radio
  or checkbox is checked or not."
  [node]
  (.-checked node))

(defn focus!
  [node]
  (.focus ^js node))

(defn get-current-uri
  []
  (u/uri (.-href js/location)))

(defn- is-portrait?
  [window]
  (let [result (.matchMedia ^js window "(orientation: portrait)")]
    (.-matches ^js result)))

(defn get-orientation
  []
  (if (is-portrait? js/window)
    :portrait
    :landscape))

(defn capture-pointer [event]
  (-> event get-target (.setPointerCapture (.-pointerId event))))

(defn release-pointer [event]
  (-> event get-target (.releasePointerCapture (.-pointerId event))))

(defn trigger-download!
  [{:keys [name href]}]
  (let [attrs {:download name  :href href}
        link  (create-element "a" attrs)
        body  (unchecked-get js/document "body")]
    (.appendChild ^js body link)
    (.click ^js link)
    (.removeChild ^js body link)))
