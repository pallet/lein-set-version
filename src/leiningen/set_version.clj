(ns leiningen.set-version
  (:require
   [clojure.string :as string])
  (:use
   [clojure.java.io :only [file]]
   [leiningen.core.main :only [debug info]]))

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
       (ex-info
        (str "Could not find " search-string " in project.clj")
        {:exit-code 1})))
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
      (throw
       (ex-info
        (str "Could not determine default version for " v)
        {:exit-code 1}))
      w)))


(defn update-version
  "Calculate new project string with the specified version."
  [project new-version prj-str]
  (let [updated-prj (reduce
                     (fn [prj-str depedency]
                       (update-dependency-version
                        project new-version depedency prj-str))
                     (update-project-version project new-version prj-str)
                     @project-versions)]
    (swap! project-versions conj (select-keys project [:name :group :version]))
    updated-prj))

(defn dec-string [^String digits]
  (let [n (dec (read-string digits))]
    (when-not (neg? n) (pr-str n))))

(defn infer-previous-version
  [{:keys [version]}]
  (let [version (string/replace version "-SNAPSHOT" "")
        components (string/split version #"\.")
        last-component (dec-string (last components))]
    (if last-component
      (string/join
       "." (conj (vec (butlast components)) last-component))
      (throw
       (ex-info
        (str "Don't know how to infer previous version from " version)
        {:exit-code 1})))))

(defn version-as-regex
  [version]
  (re-pattern (string/replace version "." "\\.")))

(defn update-file-version
  "Calculate new file string with the specified replacements."
  [project new-version
   {:keys [search-regex replace-regex no-snapshot previous-version]
    :as update}
   file-str]
  (if (or (not no-snapshot) (not (.endsWith new-version "-SNAPSHOT")))
    (let [version-regex (re-pattern
                         (version-as-regex
                          (if no-snapshot
                            (or previous-version
                                (infer-previous-version project))
                            (:version project))))
          replace-regex (or replace-regex version-regex)
          search-regex (or search-regex version-regex)]
      (debug "Searching with " search-regex)
      (loop [new-str "" file-str file-str]
        (let [matcher (re-matcher search-regex file-str)]
          (if (.find matcher)
            (let [matched (.group matcher)]
              (debug "Checking matched " matched)
              (let [n (.end matcher)
                    replacement (string/replace
                                 matched replace-regex new-version)
                    s (.replaceFirst matcher replacement)
                    nn (- (+ n (count replacement)) (count matched))]
                (recur (str new-str (subs s 0 nn)) (subs s nn))))
            (str new-str file-str)))))
    file-str))

(defn set-version
  "Update a project to the specified version."
  [project & [new-version & args]]
  (let [options (apply hash-map args)
        options (zipmap (map read-string (keys options)) (vals options))
        new-version (or new-version (default-version project))
        project-file (file (:root project) "project.clj")
        {:keys [updates] :as config} (:set-version project)]
    (spit project-file
          (update-version project new-version (slurp project-file)))
    (doseq [{:keys [path] :as update} updates
            :let [file-to-update (file (:root project) path)]]
      (debug "Updating" path)
      (if (.canRead file-to-update)
        (spit file-to-update
              (update-file-version
               project new-version (merge update options)
               (slurp file-to-update)))
        (throw
         (ex-info
          (str "Could not read file " (.getAbsolutePath file-to-update))
          {:exit-code 1}))))))
