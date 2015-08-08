(ns uxbox.dashboard.views
  (:require [uxbox.dashboard.actions :as actions]
            [uxbox.projects.actions :refer [delete-project]]
            [uxbox.dashboard.icons :as icons]
            [uxbox.icons :refer [chat logo]]
            [uxbox.user.views :refer [user]]
            [uxbox.navigation :refer [link navigate! workspace-page-route workspace-route]]
            [uxbox.time :refer [ago]]))

(defn header [db]
  [:header#main-bar.main-bar
   [:div.main-logo
    [link "/" logo]]
   [user db]])

(defn activity-item [db item]
  (let [{:keys [project
                author
                event]} item]
    [:div.activity-input
     {:key (:uuid item)}
     [:img.activity-author
      {:border "0", :src "../../images/avatar.jpg"}]
     (when (= (:type event) :create-page)
       [:div.activity-content
        [:span.bold (:name author)]
        [:span "Create new page"]
        [:div.activity-project
         [:a {:on-click #(navigate! (workspace-page-route {:project-uuid (:uuid project) :page-uuid (:page-uuid event)}))} (:name event)]
         [:span "in"]
         [:a {:on-click #(navigate! (workspace-route {:project-uuid (:uuid project)}))} (:name project)]]
        [:span.activity-time (ago (:datetime item))]])
     (when (= (:type event) :create-project)
       [:div.activity-content
        [:span.bold (:name author)]
        [:span "Create new project"]
         [:a {:on-click #(navigate! (workspace-route {:project-uuid (:uuid project)}))} (:name project)]
        [:span.activity-time (ago (:datetime item))]])
     ]))


(defn activity [db]
  [:aside#activity-bar.activity-bar
   [:div.activity-bar-inside
    [:h4 "ACTIVITY"]
    (for [[day items] (doall (seq (group-by #(.toDateString (:datetime %1)) (take 15 (:activity @db)))))]
       (concat
        [[:span.date-ribbon {:key day} (.calendar (js/moment. day))]]
        (map #(activity-item db %) items)))]])

(defn mysvg [db icon-name]
  [:svg {:src (str "/assets/images/" icon-name ".svg") }])

(defn canvas [db]
  [:section.canvas.viewport
   [mysvg db "trash"]])


(defn dashboard-info [db]
  (let [projects (vals (:projects-list @db))
        sort-order (:project-sort-order @db)
        orderings (:project-orderings @db)
        name->order (into {} (for [[k v] orderings] [v k]))
        sort-name (get orderings sort-order)]
    [:div.dashboard-info
     [:span.dashboard-projects (count projects) " projects"]
     [:span "Sort by"]
     [:select.input-select
      {:on-change #(actions/set-projects-order (name->order (.-value (.-target %))))
       :value sort-name}
      (for [order (keys orderings)
            :let [name (get orderings order)]]
        [:option {:key name} name])]]))

(defn dashboard-bar [db]
  [:section#dashboard-bar.dashboard-bar
    [dashboard-info db]
    [:div.dashboard-search
     icons/search]])

(defn new-project []
  [:div.grid-item.add-project
   {:on-click #(actions/new-project)}
   [:span "+ New project"]])

(defn project-card [project]
  (let [{:keys [uuid last-update]} project]
    [:div.grid-item.project-th {:on-click #(navigate! (workspace-route {:project-uuid uuid}))
                                :key uuid}
     [:h3
      (:name project)]
     [:span.project-th-update "Updated " (ago last-update)]
     [:div.project-th-actions
      [:div.project-th-icon.pages
       icons/page
       [:span (:pages project)]]
      [:div.project-th-icon.comments
       chat
       [:span (:comment-count project)]]
      [:div.project-th-icon.delete {:on-click #(do (.stopPropagation %) (delete-project (:uuid project)))}
       icons/trash]]]))

(defn dashboard-grid [db]
  (let [projects (vals (:projects-list @db))
        projects (sort-by (:project-sort-order @db) projects)]
    [:section.dashboard-grid
     [:h2 "Your projects"]
     [:div.dashboard-grid-content
      [new-project]
      (if (= (:project-sort-order @db) :name)
        (map project-card projects)
        (reverse (map project-card projects)))]]))

(defn dashboard-content [db]
  [:main.dashboard-main
    [:section.dashboard-content
     [dashboard-bar db]
     [dashboard-grid db]]
    [activity db]])

(defn dashboard [db]
  [:div
   [header db]
   [dashboard-content db]])
