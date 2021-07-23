;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.util.timers
  (:require
   [app.util.exceptions :as ex]))


(defn schedule
  ([func]
   (schedule 0 func))
  ([ms func]
   (let [sem (js/setTimeout #(func) ms)]
     (fn []
       (js/clearTimeout sem)))))

(defn interval
  ([f] (interval 1000 f))
  ([ms f]
   (let [counter (volatile! 1)
         semp    (volatile! nil)]
     (vreset! semp (js/setInterval (fn []
                                     (let [res (ex/ignoring (f @counter))]
                                       (vswap! counter inc)
                                       (when (= res ::stop)
                                         (js/clearInterval @semp))))
                                   ms))
     (fn []
       (js/clearInterval @semp)))))
