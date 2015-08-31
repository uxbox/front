(ns uxbox.navigation
  (:require
   [uxbox.pubsub :refer [publish!]]
   [uxbox.storage.api :as storage]
   [secretary.core :as s :refer-macros [defroute]]
   [goog.events :as events])
  (:import [goog.history Html5History]
            goog.history.EventType))

(defonce location (atom [:login]))

;; Routes

(defroute login-route "/" []
  (reset! location [:login]))

(defroute register-route "/register" []
  (reset! location [:register]))

(defroute recover-password-route "/recover-password" []
  (reset! location [:recover-password]))

(defroute dashboard-route "/dashboard" []
  (reset! location [:dashboard]))

(defroute workspace-page-route "/workspace/:project-uuid/:page-uuid" [project-uuid page-uuid]
  (reset! location [:workspace [(uuid project-uuid)
                                (uuid page-uuid)]]))

(defroute workspace-route "/workspace/:project-uuid" [project-uuid]
  (let [puuid (uuid project-uuid)]
    (reset! location [:workspace [puuid
                                  (:uuid (storage/get-first-page puuid))]])))

;; History

(def history (doto (Html5History.)
               (.setUseFragment false)
               (.setPathPrefix "")))

(defn- dispatch-uri
  [u]
  (s/dispatch! (.-token u)))

(defn navigate!
  [uri]
  (.setToken history uri))

(defn start-history!
  []
  ;; FIXME: remove as soon as we can query the data and not put it in a place
  (add-watch location
             :history
             (fn [_ _ _ new-location]
               (publish! [:location new-location])))
  (events/listen history EventType.NAVIGATE dispatch-uri)
  (.setEnabled history true)
  (s/dispatch! js/window.location.href))

;; Components

(defn link
  [href component]
  [:a
   {:href href
    :on-click #(do (.preventDefault %) (navigate! href))}
   component])
