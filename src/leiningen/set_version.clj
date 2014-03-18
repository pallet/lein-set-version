(ns leiningen.set-version
  (:require
   [clojure.string :as string])
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :refer [file]]
   [leiningen.core.main :refer [info] :as main]))

;;; Debug output on dry-run
(def dry-run false)

(defn debug [& args]
  (if dry-run
    (apply println args)
    (apply main/debug args)))

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
                       "(?s)(?m)\\(defproject\\s+%s\\s+\"(%s)\""
                       project-symbol version-string)
        search-pattern (re-pattern search-string)
        search-matcher (.matcher search-pattern prj-str)
        matched (re-find search-matcher)
        replace-pattern (re-pattern version-string)
        matcher (.matcher replace-pattern prj-str)]
    (when-not matched
      (throw
       (ex-info
        (str "Could not find " search-string " in project.clj")
        {:exit-code 1})))
    (debug "Replacing" (second matched) "in" (first matched)
           "with" new-version-string)
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
        matcher (.matcher search-pattern prj-str)
        matched (re-find search-pattern prj-str)]
    (if matched
      (do
        (debug
         "Replacing" version-string "in" matched "with" new-version-string)
        (.replaceFirst
         matcher (.replaceAll matched version-string new-version-string)))
      prj-str)))

(def version-regex
  #"(?i)([0-9]+(?:\.[0-9]+)*)(?:-(ALPHA|BETA|RC)(\.?)([0-9]+))?(-SNAPSHOT)?")

(defn version-components
  "Return a map of version components."
  [version]
  {:pre [(string? version)]}
  (if-let [[_ base pre pre-separator pre-ver snapshot]
           (re-find version-regex version)]
    {:base (->> (string/split base #"\.")
                (map edn/read-string))
     :pre pre
     :pre-separator pre-separator
     :pre-ver pre-ver
     :snapshot (boolean snapshot)}
    (throw
     (ex-info
      (str "Unrecognised version format " version)
      {:exit-code 1}))))

(defn pr-str-components
  [{:keys [base pre pre-separator pre-ver snapshot]}]
  (str (string/join "." base)
       (if pre (str "-" pre pre-separator pre-ver))
       (if snapshot "-SNAPSHOT")))

(def version-index
  {:point 1
   :minor 2
   :major 3})

(defn next-version
  [base type-kw]
  (let [n (type-kw version-index)
        prefix (reverse (drop n (reverse base)))
        v (first (drop (dec n) (reverse base)))]
    (vec (concat prefix [(inc v)] (repeat (dec n) 0)))))

(defn next-release [components type-kw]
  {:pre [(:snapshot components)]}
  (case type-kw
    :point (assoc components :snapshot false)
    (-> components
        (update-in [:base] next-version type-kw)
        (assoc :snapshot false))))

(defn next-snapshot [{:keys [base pre snapshot] :as components} type-kw]
  {:pre [(not snapshot)]}
  (if pre
    (-> components
        (dissoc :pre :pre-separator :pre-ver)
        (assoc :snapshot true))
    (-> components
        (update-in [:base] next-version type-kw)
        (assoc :snapshot true))))

(defn kw-version
  "Calculate a target version according to a keyword argument."
  [project type-kw]
  (when-not (#{:major :minor :point} type-kw)
    (throw (ex-info (str "Unknown keyword argument " type-kw ".  "
                         "Expecting one of :major, :minor or :point."))))
  (let [current (:version project)
        components (version-components current)
        new-components (if (:snapshot components)
                         (next-release components type-kw)
                         (next-snapshot components type-kw))
        new-version (pr-str-components new-components)]

        (if (= current new-version)
      (throw
       (ex-info
        (str "Could not determine default next version for " current)
        {:exit-code 1}))
      new-version)))

(defn default-version
  "Calculate a default target version by dropping any -SNAPSHOT in the
current version."
  [project]
  (kw-version project :point))

(defn process-version
  "Find a version based on the given version argument."
  [project version]
  (cond
   (not version) (default-version project)
   (.startsWith version ":") (kw-version project (keyword (subs version 1)))
   :else version))

(defn update-version
  "Calculate new project string with the specified version."
  [project new-version prj-str]
  (debug "Updating project.clj")
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
      (debug "Searching with" search-regex)
      (loop [new-str "" file-str file-str]
        (let [matcher (re-matcher search-regex file-str)]
          (if (.find matcher)
            (let [matched (.group matcher)]
              (debug "Checking matched" matched)
              (let [n (.end matcher)
                    replacement (string/replace
                                 matched replace-regex new-version)
                    s (.replaceFirst matcher replacement)
                    nn (- (+ n (count replacement)) (count matched))]
                (debug "Replacing" matched "with" replacement)
                (recur (str new-str (subs s 0 nn)) (subs s nn))))
            (str new-str file-str)))))
    file-str))

(defn set-version
  "Update a project to the specified version."
  [project & [new-version & args]]
  (let [options (apply hash-map args)
        options (zipmap (map read-string (keys options)) (vals options))
        new-version (process-version project new-version)
        project-file (file (:root project) "project.clj")
        {:keys [updates] :as config} (:set-version project)]
    (debug "dry-run" (:dry-run options))
    (alter-var-root #'dry-run (constantly (:dry-run options)))
    (when dry-run
      (debug "Dry Run - not changing any files"))
    (let [new-project (update-version project new-version (slurp project-file))]
      (when-not dry-run
        (spit project-file new-project)))
    (doseq [{:keys [path] :as update} updates
            :let [file-to-update (file (:root project) path)]]
      (debug "Updating" path)
      (if (.canRead file-to-update)
        (let [new-content (update-file-version
                           project new-version (merge update options)
                           (slurp file-to-update))]
          (when-not dry-run
            (spit file-to-update new-content)))
        (throw
         (ex-info
          (str "Could not read file " (.getAbsolutePath file-to-update))
          {:exit-code 1}))))))
