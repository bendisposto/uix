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


#_(def ui-state
    (atom
     {:current 0
      :views {[:editors 0 0] {:formalism :event-b :file "m1"}
              [:editors 0 1] {:formalism :event-b :file "m0"}
              [:editors 1 0] {:formalism :event-b :file "m2"} ;; **
              [:editors 1 1] {:formalism :event-b :file "m0"}
              [:editors 1 2] {:formalism :event-b :file "ctx0"}
              [:animations 0 0] {:editor [1 0] :model "animator0" :trace "212" :type :state-view}
              [:animations 0 1] {:editor [1 0] :model "animator0" :trace "212" :type :events-view}
              }}))

(def ui-state
  (atom
   {:current [:editors 0 0]
    :views {:editors [[:1 :2]
                      [:3 :4 :5]]
            :animators [[:6 :7]]}

    :pages {:1 {:type :editor :formalism :event-b :file "m1"}
            :2 {:type :editor :formalism :event-b :file "m0"}
            :3 {:type :editor :formalism :event-b :file "m2"}
            :4 {:type :editor :formalism :event-b :file "m0"}
            :5 {:type :editor :formalism :event-b :file "ctx0"}
            :6 {:editor :3 :model "animator0" :trace "212" :type :state-view}
            :7 {:editor [1 0] :model "animator0" :trace "212" :type :events-view}
            }}))

(defn ^:extern right []
  (let [[page row col] (:current @ui-state)
        r-size (count (get-in @ui-state [:views page row]))]
    (swap! ui-state assoc :current [page row (mod (inc col) r-size)])))

(defn ^:extern left []
  (let [[page row col] (:current @ui-state)
        r-size (count (get-in @ui-state [:views page row]))]
    (swap! ui-state assoc :current [page row (mod (dec col) r-size)])))

(defn ^:extern down []
  (let [[page row col] (:current @ui-state)
        row-cnt (count (get-in @ui-state [:views page]))
        next-row-number (mod (inc row) row-cnt)
        r-size (count (get-in @ui-state [:views page next-row-number]))
        n-col (min col (dec r-size))]
    (.log js/console "c " col " nc " n-col " nr " next-row-number " rs " r-size)
    (swap! ui-state assoc :current [page next-row-number n-col])))

(defn ^:extern up []
  (let [[page row col] (:current @ui-state)
        row-cnt (count (get-in @ui-state [:views page]))
        next-row-number (mod (dec row) row-cnt)
        r-size (count (get-in @ui-state [:views page next-row-number]))
        n-col (min col (dec r-size))]
    (.log js/console "c " col " nc " n-col " nr " next-row-number " rs " r-size)
    (swap! ui-state assoc :current [page next-row-number n-col])))


(defn show [page row column]
  (let [c (:current @ui-state)]
    (cond
      (= 3 (count c)) (when-not (= c [page row column]) {:class "hidden"})
      :otherwise {:class "col-lg-1"})))

(defmulti render-view :type)
(defmethod render-view :editor [{:keys [formalism file row column]}]
  [:div.editor (merge  {:key (str "ed" row column)}
                       (show :editors row column))
   (str "Editor: " file "@"row ","column)])

(defmethod render-view :state-view [{:keys [model trace row column]}]
  [:div.state-view (merge {:key (str "ani" row column)}
                          (show :animators row column))
   (str "State: " model "/" trace "@"row ","column)])

(defmethod render-view :events-view [{:keys [model trace row column]}]
  [:div.events-view (merge {:key (str "ani" row column)}
                           (show :animators row column))
   (str "Events: " model "/" trace "@"row ","column)])

(defmethod render-view :history-view [{:keys [model trace row column]}]
  [:div.history-view (merge {:key (str "ani" row column)}
                            (show :animators row column))
   (str "History: " model "/" trace "@"row ","column)])


(defn render-row [y columns]
  [:div.row {:key (str "row" y)}
   (doall (map-indexed (fn [x key] (render-view (assoc (get-in @ui-state [:pages key]) :column x :row y))) columns))])

(defn render-page [[section rows]]
  [:div.page (merge {:key section}
                    (when-not (= section (first (:current @ui-state))) {:class "hidden"}))
   (doall (map-indexed render-row rows))
   ])


(defn home-page []
  (let [views (:views @ui-state)]
    [:div.ui
     (doall (map render-page views))]))


;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
