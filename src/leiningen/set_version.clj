(ns leiningen.set-version
  (:require
   [clojure.string :as string])
  (:use
   [clojure.java.io :only [file]]))

;;; enable subproject dependency updates
(def ^:private project-versions (atom []))

(defn project-symbol-string
  "Return the symbol for the project as a string"
  [project]
  (if (= (:group project) (:name project))
    (:name project)
    (str (:group project) "/" (:name project))))

(defn update-project-version
  "Update the project version"
  [project new-version-string prj-str]
  (let [project-symbol (project-symbol-string project)
        version-string (:version project)
        search-string (format
                       "(?s)(?m)\\(defproject\\s+%s\\s+\"%s\""
                       project-symbol version-string)
        search-pattern (re-pattern search-string)
        replace-pattern (re-pattern version-string)
        matcher (.matcher replace-pattern prj-str)]
    (when-not (re-find search-pattern prj-str)
      (throw
       (RuntimeException.
        (format "Could not find %s in project.clj" search-string))))
    (.replaceFirst matcher new-version-string)))

(defn update-dependency-version
  "Update a dependency version."
  [project new-version-string dependency prj-str]
  (let [project-symbol (project-symbol-string dependency)
        version-string (:version project)
        search-string (format
                       "(?s)(?m)\\[\\s*%s\\s+\"%s\"[^\\]]*\\]"
                       project-symbol version-string)
        search-pattern (re-pattern search-string)
        replace-pattern (re-pattern version-string)
        matcher (.matcher search-pattern prj-str)]
    (if-let [dep (re-find search-pattern prj-str)]
      (.replaceFirst
       matcher (.replaceAll dep version-string new-version-string))
      prj-str)))

(defn default-version
  "Calculate a default target version by dropping any -SNAPSHOT in the
current version."
  [project]
  (let [v (:version project)
        w (string/replace v "-SNAPSHOT" "")]
    (if (= v w)
      (throw (RuntimeException.
              (str "Could not determine default version for " v)))
      w)))

(defn update-version
  "Calculate new project string with the specified version."
  [project version prj-str]
  (let [version (or version (default-version project))
        updated-prj (reduce
                     (fn [prj-str depedency]
                       (update-dependency-version
                        project version depedency prj-str))
                     (update-project-version project version prj-str)
                     @project-versions)]
    (swap! project-versions conj (select-keys project [:name :group :version]))
    updated-prj))

(defn set-version
  "Update a project to the specified version."
  [project & [version]]
  (let [project-file (file (:root project) "project.clj")]
    (spit project-file
     (update-version project version (slurp project-file)))))
