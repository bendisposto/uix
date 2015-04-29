(ns uix.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [uix.lorem :as l])
  (:import goog.History))

;; -------------------------
;; Views


(def ui-state
  (atom
   {:current [:editors 0 0]
    :views {:editors [[:1 :2]
                      [:3 :4 :5]]
            :animators [[:8 :6 :7]]}

    :pages {:1 {:type :editor :formalism :classical-b :kind :machine :file "m1"}
            :2 {:type :editor :formalism :classical-b :kind :machine :file "m0"}
            :3 {:type :editor :formalism :event-b :kind :machine :file "m2"}
            :4 {:type :editor :formalism :event-b :kind :machine :file "m0"}
            :5 {:type :editor :formalism :event-b :kind :context :file "ctx0"}
            :6 {:editor :3 :model "animator0" :trace "212" :type :state-view}
            :7 {:editor :3 :model "animator0" :trace "212" :type :events-view}
            :8 {:editor :3 :model "animator0" :trace "212" :type :history-view}
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

(defn ^:extern esc []
  (swap! ui-state (fn [x] (assoc x :overview? true))))

(defn ^:extern desktop []
  (swap! ui-state
         (fn [{[page row column] :current :as x}]
           (assoc x :current [({:editors :animators, :animators :editors} page) 0 0]))))

(defn ^extern select
  ([] (let [[_ r c] (:current @ui-state)]
        (select r c)))
  ([row column]
   (swap! ui-state
          (fn [x]
            (assoc x
                   :overview? false
                   :current (into [(first (:current x))] [row column]))))))

(defn show [shown? page row column]
  (let [c (:current @ui-state)]
    (when-not (and shown? (= c [page row column])) {:class " hidden "})))

(defmulti render-thumbnail (fn [e _] (:type e)))

(defmethod render-thumbnail :editor [{:keys [formalism file kind row column]} shown?]
  [:div.thumb.editor-thumbnail
   {:key (str "tned-" row "-" column)
    :class (str (if shown? "" " hidden ")
                (if (= 0 column) " main-component " "")
                "tn-" (name formalism) "-" (name kind)
                (if (= [row column] (rest (:current @ui-state))) " selected " ""))
    :on-click #(select row column)}
   [:div.editor-thumbnail-text {:key (str "tnedtext-" row "-" column) } file]]
  )

(defmethod render-thumbnail :default [{:keys [model type row column]} shown?]
  [:div.thumb.view-thumbnail
   {:key (str "tnv-" row "-" column)
    :class (str (if shown? "" " hidden ")
                "tn-" (name type)
                (if (= [row column] (rest (:current @ui-state))) " selected " ""))
    :on-click #(select row column)}])

(defmulti render-view (fn [e _] (:type e)))

(defmethod render-view :editor [{:keys [formalism file row column]} _]
  (let [cm (atom nil)
        content (atom (l/gen-lorem))
        editor-id (str "editor-" row "-" column)]
    (reagent/create-class
     {
      :component-did-mount (fn [c]
                             (let [dom-element (.getElementById js/document editor-id)
                                   mirr (.fromTextArea
                                         js/CodeMirror
                                         dom-element #js {:lineWrapping true
                                                          :lineNumbers true})]
                               (reset! cm mirr)))
      :component-did-update (fn [e]
                              (let [doc (.-doc @cm)]
                                (.setValue doc @content)))
      :reagent-render
      (fn [editor shown?]
        (let []
          [:div.editor (merge  {:key (str "ed" row column)}
                               (show shown? :editors row column))
           [:h1 (str "Editor: " file "@"row ","column)]
           [:textarea {:id editor-id :defaultValue @content}]]))})))

(defmethod render-view :state-view [{:keys [model trace row column]} shown?]
  [:div.state-view (merge {:key (str "ani" row column)}
                          (show shown? :animators row column))
   [:h1 (str "State: " model "/" trace "@"row ","column)]
   [:p (l/gen-lorem 2)]
   [:div (l/gen-lorem 3)]])

(defmethod render-view :events-view [{:keys [model trace row column]} shown?]
  [:div.events-view (merge {:key (str "ani" row column)}
                           (show shown? :animators row column))
   [:h1 (str "Events: " model "/" trace "@"row ","column)]
   [:i (l/gen-lorem 4)]
   [:div (l/gen-lorem 5)]])

(defmethod render-view :history-view [{:keys [model trace row column]} shown?]
  [:div.history-view (merge {:key (str "ani" row column)}
                            (show shown? :animators row column))
   [:h1 (str "History: " model "/" trace "@"row ","column)]
   [:b (l/gen-lorem 6)]
   [:p (l/gen-lorem 7)]])

(defn render-row [y columns]
  [:div.row {:key (str "row" y)}
   (doall (map-indexed
           (fn [x key]
             (let [e (assoc (get-in @ui-state [:pages key]) :column x :row y)
                   overview? (:overview? @ui-state)
                   page (:cpage @ui-state)]
               [:div {:key (str  "view-elem-" x y)} (render-thumbnail e overview?)
                [render-view e (not overview?)]]))
           columns))])

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
  (.add js/shortcut "Alt+Left" #(left))
  (.add js/shortcut "Alt+Right" #(right))
  (.add js/shortcut "Alt+Up" #(up))
  (.add js/shortcut "Alt+Down" #(down))
  (.add js/shortcut "Escape" #(esc))
  (.add js/shortcut "Meta+Left" #(desktop))
  (.add js/shortcut "Meta+Right" #(desktop))
  (.add js/shortcut "Enter" #(select))
  (mount-root))
