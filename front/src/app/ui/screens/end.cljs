;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.end
  (:require
   [app.store :as st]
   [app.util.data :as d]
   [app.ui.icons :as i]
   [app.ui.avatars :refer [avatar]]
   [app.ui.common :as cm]
   [app.ui.context :as ctx]
   [app.util.object :as obj]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [promesa.core :as p]
   [potok.core :as ptk]
   [goog.events :as gev]
   [rumext.alpha :as mf]))

(defn- create-image
  [item]
  (p/create
   (fn [resolve]
     (let [width  (get item "width")
           height (get item "height")
           src    (get item "dataURI")

           img    (-> (js/Image.)
                      (obj/set! "src" src)
                      (obj/set! "width" width)
                      (obj/set! "height" height))]

       (gev/listenOnce img "load"
                       #(resolve {:image img
                                  :width width
                                  :height height}))))))
(defn- render-images!
  [ctx images]
  (loop [y      0
         images images]
    (when-let [{:keys [image width height]} (first images)]
      (.drawImage ^js ctx image 0 y width height)
      (recur (+ y height)
             (rest images)))))

(mf/defc end-screen
  {::mf/wrap [mf/memo]}
  [{:keys [game index]}]
  (let [session-id (mf/use-ctx ctx/session-id)
        wsock      (mf/use-ctx ctx/wsocket)

        drawings   (get game "drawings")
        index      (js/parseInt (or index 0))
        group      (get drawings index)

        data-uri   (mf/use-state nil)

        on-download
        (fn [event]
          (when-let [data-uri (deref data-uri)]
            (wa/trigger-download! {:name (str "drawing-" (inc index) ".png")
                                   :href data-uri})))

        on-next
        (fn [event]
          (when (< index (dec (count drawings)))
            (prn "next-index" (inc index))
            (swap! st/state assoc-in [:params :index] (inc index))))

        on-prev
        (fn [event]
          (when (pos? index)
            (swap! st/state assoc-in [:params :index] (dec index))))]

    (mf/use-effect
     (mf/deps group)
     (fn []
       (let [elements (map #(get-in game ["canvas" %]) group)
             height   (reduce + 0 (map #(get % "height") elements))
             width    (-> elements first (get "width"))

             node     (wa/create-element "canvas" {:width width :height height})
             ctx      (wa/get-context node "2d")]

         (-> (p/all  (map create-image elements))
             (p/then (fn [images]
                       (render-images! ctx images)
                       (reset! data-uri (.toDataURL node "image/png"))))))))

    [:*
     [:div.header]
     [:div.main-content
      [:& cm/left-sidebar]
      [:div.main-panel
       [:div.drawing
        [:div.drawing-container
         (when @data-uri
           [:img {:src @data-uri}])]]]

      [:div.right-sidebar
       [:div.spacer]
       [:div.navigation
        [:div {:on-click on-prev} [:& i/left]]
        [:div {:on-click on-next} [:& i/right]]]
       [:div.download {:on-click on-download}
        [:div [:& i/download]]]]]
     [:div.footer]
     ]))
