(ns uix.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react])
  (:import goog.History))

;; -------------------------
;; Views


(def ui-state
  (atom
   {:current-page 0
    :current-view [0 0 0]
    :views [
     [
      [[{:type :editor :formalism :event-b :file "m1"}
        {:type :editor :formalism :event-b :file "m0"}]]
      [[{:type :editor :formalism :classical-b :file "s1.mch"}]]
      ]
     [[[{:model "animation0" :trace "1" :editor 0 :type :state-view}
        {:model "animation0" :trace "1" :editor 0 :type :events-view}]
       [{:model "animation0" :trace "2" :editor 0 :type :state-view}
        {:model "animation0" :trace "2" :editor 0 :type :history-view}]]
      [[{:model "animation3" :trace "9" :editor 1 :type :events-view}]]]]
    }))

(defmulti render-view :type)
(defmethod render-view :editor [{:keys [formalism file]}]
  [:div.editor (str "Editor: " file)])

(defmethod render-view :state-view [{:keys [model trace]}]
  [:div.state-view (str "State: " model "/" trace)])

(defmethod render-view :events-view [{:keys [model trace]}]
  [:div.events-view (str "Events: " model "/" trace)])

(defmethod render-view :history-view [{:keys [model trace]}]
  [:div.history-view (str "History: " model "/" trace)])


(defn render-subrow [elements]
  [:div.subrow (map render-view elements)])

(defn render-row [subrows]
  [:div.row (map render-subrow subrows)])

(defn render-page [current index rows]
  (.log js/console current index (count rows))
  [:div.page
   (when-not (= current index) {:class "hidden"})
   (map render-row rows)])

(defn ^:extern animation->editor []
  (swap! ui-state
         (fn [{:keys [current-page current-view views] :as s}
             {:keys [editor]} views]
           (assoc s :current-view [editor 0 0] :current-page 0))))

(defn ^:extern editor->animation []
  (swap! ui-state
         (fn [{:keys [current-page current-view views] :as s}
             {:keys [editor]} views]
           (assoc s :current-view [editor 0 0] :current-page 0))))

(defn home-page []
  (let [{:keys [current-page views current-view]} @ui-state]
    [:div.ui
     (map-indexed (partial render-page current-page current-view) views)]))


;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
