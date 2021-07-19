;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.util.spec
  "Data manipulation and query helper functions."
  (:refer-clojure :exclude [assert])
  #?(:cljs (:require-macros [app.util.spec :refer [assert]]))
  (:require
   #?(:clj  [clojure.spec.alpha :as s]
      :cljs [cljs.spec.alpha :as s])
   [expound.alpha :as expound]
   [app.util.data :as d]
   [app.util.exceptions :as ex]
   [cuerdas.core :as str]))

(s/check-asserts true)

;; --- SPEC: boolean

(letfn [(conformer [v]
          (if (boolean? v)
            v
            (if (string? v)
              (if (re-matches #"^(?:t|true|false|f|0|1)$" v)
                (contains? #{"t" "true" "1"} v)
                ::s/invalid)
              ::s/invalid)))
        (unformer [v]
          (if v "true" "false"))]
  (s/def ::boolean (s/conformer conformer unformer)))

;; --- SPEC: number

(letfn [(conformer [v]
          (cond
            (number? v)      v
            (str/numeric? v) #?(:cljs (js/parseFloat v)
                                :clj  (Double/parseDouble v))
            :else            ::s/invalid))]
  (s/def ::number (s/conformer conformer str)))

;; --- SPEC: integer

(letfn [(conformer [v]
          (cond
            (integer? v) v
            (string? v)
            (if (re-matches #"^[-+]?\d+$" v)
              #?(:clj (Long/parseLong v)
                 :cljs (js/parseInt v 10))
              ::s/invalid)
            :else ::s/invalid))]
  (s/def ::integer (s/conformer conformer str)))

;; --- SPEC: keyword

(letfn [(conformer [v]
          (cond
            (keyword? v) v
            (string? v)  (keyword v)
            :else        ::s/invalid))

        (unformer [v]
          (d/name v))]
  (s/def ::keyword (s/conformer conformer unformer)))


;; --- SPEC: set-of-kw

(letfn [(conformer [v]
          (cond
            (string? v) (into #{} (map keyword) (str/words v))
            (vector? v) (into #{} (map str/keyword) v)
            (set? v)    (into #{} (filter keyword?) v)
            :else       ::s/invalid))
        (unformer [v]
          (into #{} (map d/name) v))]
  (s/def ::set-of-kw (s/conformer conformer unformer)))

;; --- SPEC: set-of-str

(letfn [(conformer [v]
          (cond
            (string? v) (into #{} (map str/trim) (str/split v #","))
            (vector? v) (into #{} v)
            (set? v)    (into #{} (filter string?) v)
            :else       ::s/invalid))
        (unformer [v] v)]
  (s/def ::set-of-str (s/conformer conformer unformer)))


;; --- Default Specs

(s/def ::inst inst?)
(s/def ::string string?)
(s/def ::not-empty-string (s/and string? #(not (str/empty? %))))
(s/def ::url string?)
(s/def ::fn fn?)

;; --- Macros

(defn spec-assert*
  [spec x message context]
  (if (s/valid? spec x)
    x
    (let [data    (s/explain-data spec x)
          explain (with-out-str (s/explain-out data))]
      (ex/raise :type :assertion
                :code :spec-validation
                :hint message
                :data data
                :explain explain
                :context context
                #?@(:cljs [:stack (.-stack (ex-info message {}))])))))


(defmacro assert
  "Development only assertion macro."
  [spec x]
  (when *assert*
    (let [nsdata  (:ns &env)
          context (when nsdata
                    {:ns (str (:name nsdata))
                     :name (pr-str spec)
                     :line (:line &env)
                     :file (:file (:meta nsdata))})
          message (str "Spec Assertion: '" (pr-str spec) "'")]
      `(spec-assert* ~spec ~x ~message ~context))))

(defmacro verify
  "Always active assertion macro (does not obey to :elide-asserts)"
  [spec x]
  (let [nsdata  (:ns &env)
        context (when nsdata
                  {:ns (str (:name nsdata))
                   :name (pr-str spec)
                   :line (:line &env)
                   :file (:file (:meta nsdata))})
        message (str "Spec Assertion: '" (pr-str spec) "'")]
    `(spec-assert* ~spec ~x ~message ~context)))

;; --- Public Api

(defn conform
  [spec data]
  (let [result (s/conform spec data)]
    (when (= result ::s/invalid)
      (let [data    (s/explain-data spec data)
            explain (with-out-str
                      (s/explain-out data))]
        (throw (ex/error :type :validation
                         :code :spec-validation
                         :explain explain
                         :data data))))
    result))

(defmacro instrument!
  [& {:keys [sym spec]}]
  (when *assert*
    (let [message (str "Spec failed on: " sym)]
      `(let [origf# ~sym
             mdata# (meta (var ~sym))]
         (set! ~sym (fn [& params#]
                      (spec-assert* ~spec params# ~message mdata#)
                      (apply origf# params#)))))))

