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
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [promesa.core :as p]
   [potok.core :as ptk]
   [goog.events :as gev]
   [rumext.alpha :as mf]))


(defn create-image
  [item]
  (p/create
   (fn [resolve]
     (let [width  (get item "width")
           height (get item "height")
           src    (get item "dataURI")
           img    (js/Image.)]
       ;; (js/console.log (pr-str item))

       (set! (.-src img) src)
       (set! (.-width img) width)
       (set! (.-height img) height)
       (gev/listenOnce img "load" #(resolve {:image img
                                             :width width
                                             :height height}))))))


(defn render-images
  [ctx images]
  (loop [y      0
         images images]
    (when-let [{:keys [image width height]} (first images)]
      (.drawImage ^js ctx image 0 y width height)
      (recur (+ y height)
             (rest images)))))

(mf/defc drawing-item
  [{:keys [game group]}]
  (let [duri (mf/use-state nil)]
    (mf/use-effect
     (fn []
       (let [elements (map #(get-in game ["canvas" %]) group)
             height   (reduce + 0 (map #(get % "height") elements))
             width    (-> elements first (get "width"))
             node     (.createElement js/document "canvas")
             ctx      (.getContext ^js node "2d")]
         ;; (cljs.pprint/pprint elements)

         (set! (.-width node) width)
         (set! (.-height node) height)
         (-> (p/all (map create-image elements))
             (p/then (partial render-images ctx))
             (p/then (fn []
                       (reset! duri (.toDataURL node "image/png"))))))))

    [:div.drawing-item
     [:div.drawing-container
      (when @duri
        [:img {:src @duri}])]]))


(mf/defc end-screen
  {::mf/wrap [mf/memo]}
  [{:keys [game params]}]
  (let [session-id (mf/use-ctx ctx/session-id)
        wsock      (mf/use-ctx ctx/wsocket)

        drawings   (get game "drawings")
        index      (get params :image 0)
        group      (get drawings index)]

    [:*
     [:div.header]
     [:div.main-content
      [:& cm/left-sidebar]
      [:div.main-panel
       [:& drawing-item {:game game :group group}]]
      [:div.right-sidebar
       [:div.spacer]
       [:div.navigation
        [:div [:& i/left]]
        [:div [:& i/right]]]
       [:div.download
        [:div [:& i/download]]]]]
     [:div.footer]
     ]))
