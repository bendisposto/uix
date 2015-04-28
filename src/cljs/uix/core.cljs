(ns uix.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react])
  (:import goog.History))

;; -------------------------
;; Views


(def ui-state
  (r/atom
   {:current {:desktop "editor-desktop"
              :view nil}
    
    :editors [{:text "scheduler"
                :id 1
                :type :model
                :formalism :classical-b
                :content :e1}

               {:text "Scheduler0"
                :type :editor
                :id 2
                :formalism :event-b
                :content :e2}]

    :animations [{:text "Animator0"
                  :editor 1
                  :type :animation
                  :id "animator0"
                  :sub-rows [:t1 :t2]}
                 {:text "Scheduler 0"
                  :editor 2
                  :type :animation
                  :id "animator1"
                  :sub-rows [:t3]}]

    :rows {:t1 [{:id 5
                 :type :state-view}
                {:id 6
                 :type :events-view}]
           :t2 [{:id 7
                 :type :state-view}
                {:id 8
                 :type :events-view}]
           :t3 [{:id 9
                 :type :history-view}]
           :e1 [{:id 10
                 :type :editor
                 :file "m1"}
                {:id 11
                 :type :editor
                 :file "m0"}]
           :e2 [{:id 12
                 :type :editor
                 :file "Scheduler0.bum"}]}}))

(defn mk-editor-view [focused? {:keys [id file]}]
  (let [dom-id (str "editor-" id)]
    [:div.editor {:id dom-id :key dom-id :class (if focused? "focused" "thumbnail")}
     [:p "Edit: " file]]))

(defn mk-editor-row [{:keys [text id formalism content]}]
  (let [dom-id (str "editor-row-" id)]
    [:div.editor-row {:id dom-id :key dom-id}
     (map (partial mk-editor-view false) (get-in @ui-state [:rows content]))]))

(defn mk-editor-desktop [focus editors]
  (let [id "editor-desktop"
        show? ()]
    [:div.desktop {:id id :key id :class (if shown? "focused" "hidden")}
     [:h1 "Editors"] 
     (if focused? (map mk-editor-row editors)
         ())]))

(defmulti mk-animaton-view (fn [_ x] (:type x)))

(defmethod mk-animaton-view :state-view [focus {:keys [id]}]
  (let [dom-id (str "animation-view-" id)
        focused? (= dom-id focus)]
    [:div.view.state-view {:id dom-id :key dom-id :class (when focused? "focus")}
     "State-View"]))

(defmethod mk-animaton-view :events-view [focus {:keys [id]}]
  (let [dom-id (str "animation-view-" id)
        focused? (= dom-id focus)]
    [:div.view.events-view {:id dom-id :key dom-id :class (when focused? "focus")}
     "Events-View"]))

(defmethod mk-animaton-view :history-view [focus {:keys [id]}]
  (let [dom-id (str "animation-view-" id)
        focused? (= dom-id focus)]
    [:div.view.history-view {:id dom-id :key dom-id :class (when focused? "focus")}
     "History-View"]))

(defn mk-trace-row [focus key]
  (let [gui-id (str "trace-" (name key))
        views (get-in @ui-state [:rows key])]
    [:div.trace-row {:id gui-id :key gui-id}
     (map (partial mk-animaton-view focus) views)]))

(defn mk-animator-row [focus {:keys [text editor id sub-rows]}]
  (let [dom-id (str "animator-" id)]
    [:div.animator-row {:id dom-id :key dom-id}
     (map (partial mk-trace-row focus) sub-rows)]))

(defn mk-animation-desktop [focus animators]
  (let [id "animation-desktop"
        focused? (= id (last focus))] 
    [:div.desktop {:id id :key id :class (if focused? "focused" "hidden")}
     [:h1 "Animations"]
     (map (partial mk-animator-row (last focus)) animators)]))

(defn home-page []
  (fn []
    (let [editors (:editors @ui-state)
          animations (:animations @ui-state)
          focus (:focus @ui-state)]
      [:div
       (mk-editor-desktop focus editors)
       (mk-animation-desktop focus animations)])))

(defn about-page []
  [:div [:h2 "About uix"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (r/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
