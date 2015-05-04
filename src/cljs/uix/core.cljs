(ns uix.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [re-frame.core :as rf]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [uix.lorem :as l])
  (:import goog.History))

;; -------------------------
;; Views

(defn init-state []
  {:current [:editors 0 0]
   :views {:editors [[:1 :2]
                     [:3 :4 :5]]
           :animators [[:8 :6 :7]]}
   :overview? false
   :pages {:1 {:type :editor :formalism :classical-b :kind :machine :file "m1"}
           :2 {:type :editor :formalism :classical-b :kind :machine :file "m0"}
           :3 {:type :editor :formalism :event-b :kind :machine :file "m2"}
           :4 {:type :editor :formalism :event-b :kind :machine :file "m0"}
           :5 {:type :editor :formalism :event-b :kind :context :file "ctx0"}
           :6 {:editor :3 :model "animator0" :trace "212" :type :state-view}
           :7 {:editor :3 :model "animator0" :trace "212" :type :events-view}
           :8 {:editor :3 :model "animator0" :trace "212" :type :history-view}
           }})

(rf/register-handler
 :init-db
 (fn [db _]
   (init-state)))

(rf/register-sub
 :current
 (fn [db] (reaction (:current @db))))

(rf/register-sub
 :view
 (fn [db] (reaction (:views @db))))

(rf/register-sub
 :pages
 (fn [db] (reaction (:pages @db))))

(rf/register-sub
 :overview?
 (fn [db] (reaction (:overview? @db))))

(rf/register-sub
 :current-page
 (fn [db] (reaction (first (:current @db)))))

(rf/register-sub
 :current-row
 (fn [db] (reaction (second (:current @db)))))

(rf/register-sub
 :current-column
 (fn [db] (reaction (last (:current @db)))))


(defn left-right [{[page row column] :current views :views :as db} f]
  (let [r-size (count (get-in views [page row]))]
    (assoc db :current [page row (mod (f column) r-size)])))


(defn up-down [{[page row column] :current views :views :as db} f]
  (let [rc (count (get views page))
        nr (mod (f row) rc)
        rs (dec (count (get-in views [page nr])))
        nc (min column rs)]
    (assoc db :current [page nr nc])))

(defn desktop [{[page row column] :current :as db}]
  (assoc db :current [({:editors :animators, :animators :editors} page) 0 0]))

(defn esc [db]
  (assoc db :overview? true))

(defn select-view
  ([{[_ r c] :current :as db}] (select-view db r c))
  ([{[p r c] :current :as db} row column]
   (assoc db
          :overview? false
          :current [p row column])))

(rf/register-handler
 :navigate
 rf/debug
 (fn [db [_ direction]]
   (if (:overview? db)
     (desktop db)
     (case direction
       :up (up-down db dec)
       :down (up-down db inc)
       :left (left-right db dec)
       :right (left-right db inc)))))

(rf/register-handler
 :navigate-ov
 (fn [db [_ direction]]
   (if (:overview? db)
     (case direction
       :up (up-down db dec)
       :down (up-down db inc)
       :left (left-right db dec)
       :right (left-right db inc))
     db)))

(rf/register-handler
 :esc
 (fn [db _]
   (if (:overview? db)
     (select-view db)
     (esc db))))

(rf/register-handler
 :enter
 (fn [db _] (if (:overview? db) (select-view db) db)))

(rf/register-handler
 :select
 (fn [db [_ row column]] (select-view db row column)))

(defn show [shown? page row column]
  (let [c (rf/subscribe [:current])]
    (when-not (and shown? (= @c [page row column])) {:class " hidden "})))

(defmulti render-thumbnail (fn [e _] (:type e)))

(defmethod render-thumbnail :editor [{:keys [formalism file kind row column]} shown?]
  (let [c (rf/subscribe [:current])]
    [:div.thumb.editor-thumbnail
     {:key (str "tned-" row "-" column)
      :class (str (if shown? "" " hidden ")
                  (if (= 0 column) " main-component " "")
                  "tn-" (name formalism) "-" (name kind)
                  (if (= [row column] (rest @c)) " selected " ""))
      :on-click #(rf/dispatch [:select row column])}
     [:div.editor-thumbnail-text {:key (str "tnedtext-" row "-" column) } file]])
  )

(defmethod render-thumbnail :default [{:keys [model type row column]} shown?]
  (let [c (rf/subscribe [:current])]
    [:div.thumb.view-thumbnail
     {:key (str "tnv-" row "-" column)
      :class (str (if shown? "" " hidden ")
                  "tn-" (name type)
                  (if (= [row column] (rest @c)) " selected " ""))
      :on-click #(rf/dispatch [:select row column])}]))

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
             (let [pages (rf/subscribe [:pages])
                   e (assoc (get @pages key) :column x :row y)
                   overview? (rf/subscribe [:overview?])]
               [:div {:key (str  "view-elem-" x y)}
                (render-thumbnail e @overview?)
                [render-view e (not @overview?)]]))
           columns))])

(defn render-page [[section rows]]
  (let [page (rf/subscribe [:current-page])]
    [:div.page (merge {:key section}
                      (when-not (= section @page) {:class "hidden"}))
     (doall (map-indexed render-row rows))
     ]))

(defn home-page []
  (let [views (rf/subscribe [:view])]
    [:div.ui
     (doall (map render-page @views))]))


;; -------------------------
;; Initialize app

(rf/register-handler
 :mount
 (fn [db _]
   (reagent/render [home-page] (.getElementById js/document "app"))
   db))

(defn init! []
  (.add js/shortcut "Shift+Left" #(rf/dispatch [:navigate :left]))
  (.add js/shortcut "Shift+Right" #(rf/dispatch [:navigate :right]))
  (.add js/shortcut "Shift+Up" #(rf/dispatch [:navigate :up]))
  (.add js/shortcut "Shift+Down" #(rf/dispatch [:navigate :down]))
  (.add js/shortcut "Left" #(rf/dispatch [:navigate-ov :left]))
  (.add js/shortcut "Right" #(rf/dispatch [:navigate-ov :right]))
  (.add js/shortcut "Up" #(rf/dispatch [:navigate-ov :up]))
  (.add js/shortcut "Down" #(rf/dispatch [:navigate-ov :down]))
  (.add js/shortcut "Escape" #(rf/dispatch [:esc]))
  (.add js/shortcut "Enter" #(rf/dispatch [:enter]))
  (rf/dispatch [:init-db])
  (rf/dispatch [:mount]))
