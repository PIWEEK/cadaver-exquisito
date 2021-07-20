;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui
  (:require
   [app.config :as cf]
   [app.events]
   [app.store :as st]
   [app.util.data :as d]
   [app.util.webapi :as wa]
   [app.ui.context :as ctx]
   [app.ui.screens.start :refer [start-screen]]
   [app.ui.screens.room :refer [room-screen]]
   [app.ui.screens.draw :refer [draw-screen]]
   [cljs.pprint :refer [pprint]]
   [app.ui.rhooks :as rh]
   [cuerdas.core :as str]
   [goog.events :as events]
   [expound.alpha :as expound]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(mf/defc app
  [props]
  (when-let [nav (mf/deref st/nav-ref)]
    (let [orientation (rh/use-orientation)]
      [:main
       [:div.layout
        (if (= :portrait orientation)
          [:div.notice "Put the device in landscape orientation."]
          [:*
           ;; [:div.left-sidebar
           ;;  [:div.hname "cadaver exquisito"]]
           [:div.screen {:class (str "screen-" (name (:screen nav)))}
            (case (:screen nav)
              :start [:& start-screen]
              :room [:& room-screen]
              :draw [:& draw-screen]
              [:span "not found"])]])]])))

;; This is a pure frontend error that can be caused by an active
;; assertion (assertion that is preserved on production builds). From
;; the user perspective this should be treated as internal error.
(defmethod ptk/handle-error :assertion
  [{:keys [data stack message context] :as error}]
  ;; (ts/schedule
  ;;  (st/emitf (dm/show {:content "Internal error: assertion."
  ;;                      :type :error})))

  ;; Print to the console some debugging info
  (js/console.group message)
  (js/console.info (str/format "ns: '%s'\nname: '%s'\nfile: '%s:%s'"
                                (:ns context)
                                (:name context)
                                (str cf/public-uri "/js/cljs-runtime/" (:file context))
                                (:line context)))
  (js/console.groupCollapsed "Stack Trace")
  (js/console.info stack)
  (js/console.groupEnd "Stack Trace")
  (js/console.error (with-out-str (expound/printer data)))
  (js/console.groupEnd message))

;; Error that happens on an active bussines model validation does not
;; passes an validation (example: profile can't leave a team). From
;; the user perspective a error flash message should be visualized but
;; user can continue operate on the application.
(defmethod ptk/handle-error :validation
  [error]
  ;; (ts/schedule
  ;;  (st/emitf
  ;;   (dm/show {:content "Unexpected validation error (server side)."
  ;;             :type :error})))

  ;; Print to the console some debug info.
  (js/console.group "Validation Error")
  (js/console.info
   (with-out-str
     (pprint (dissoc error :explain))))
  (when-let [explain (:explain error)]
    (js/console.error explain))
  (js/console.groupEnd "Validation Error"))

(defmethod ptk/handle-error :default
  [error]
  (if (instance? ExceptionInfo error)
    (ptk/handle-error (ex-data error))
    (do
      #_(ts/schedule
       (st/emitf (dm/assign-exception error)))

      (js/console.group "Internal error:")
      (js/console.log "hint:" (or (ex-message error)
                                  (:hint error)
                                  (:message error)))
      (js/console.error (clj->js error))
      (js/console.error "stack:" (.-stack error))
      (js/console.groupEnd "Internal error:"))))

(defonce uncaught-error-handler
  (letfn [(on-error [event]
            (ptk/handle-error (unchecked-get event "error"))
            (.preventDefault ^js event))]
    (.addEventListener js/window "error" on-error)
    (fn []
      (.removeEventListener js/window "error" on-error))))
