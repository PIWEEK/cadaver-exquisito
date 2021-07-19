;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.store
  (:require
   [app.util.data :as d]
   [app.util.storage :refer [storage]]
   [app.util.timers :as tm]
   [beicon.core :as rx]
   [cuerdas.core :as str]
   [lambdaisland.glogi :as log]
   [okulary.core :as l]
   [potok.core :as ptk]))

(log/set-level 'app.store :info)


(defonce state  (ptk/store {:resolve ptk/resolve
                            :state (::state storage)}))
(defonce stream (ptk/input-stream state))

(defonce debug-subscription
  (->> stream
       (rx/filter ptk/event?)
       (rx/subs #(log/trace :source "stream"
                            :event (ptk/type %)
                            :data (when (satisfies? IDeref %)
                                    (deref %))))))


(defmethod ptk/resolve :default
  [type params]
  (ptk/data-event type params))

(def nav-ref
  (l/derived :nav state))

(def message-ref
  (l/derived :message state))

(defn emit!
  ([] nil)
  ([event]
   (ptk/emit! state event)
   nil)
  ([event & events]
   (apply ptk/emit! state (cons event events))
   nil))

(defn- on-change
  [state]
  (swap! storage assoc ::state (dissoc state :nav)))

(defn init
  "Initialize the state materialization."
  ([] (init {}))
  ([props]
   (add-watch state ::persistence #(on-change %4))
   (let [state (::state storage)]
     (emit! #(d/merge % state props)))))
