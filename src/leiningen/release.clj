(ns leiningen.release
  "Automatically bump your project's semantic version at release-time."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as string]
            [leiningen.core.main :refer [apply-task]])
  (:import [java.util.regex Pattern]))

(defn raise [fmt & args]
  (throw (RuntimeException. (apply format fmt args))))

(def ^:dynamic config {:clojars-url "clojars@clojars.org:"})
(def ^:dynamic jar-name "")

(def default-config {:release-tasks [:jar :pom :install]})

(def ^:dynamic *scm-systems*
     {:git {:add    ["git" "add"]
            :tag    ["git" "tag"]
            :commit ["git" "commit"]
            :push   ["git" "push" "origin" "master"]
            :status ["git" "status"]}})

(defn detect-scm []
  (or
   (:scm config)
   (cond
     (.exists (java.io.File. ".git"))
     :git
     :no-scm-detected
     (raise "Error: no scm detected! (I know only about git for now)."))))

(defn sh! [& args]
  (let [res (apply sh/sh args)]
    (.println System/err (:err res))
    (when-not (zero? (:exit res))
      (raise "Error: command failed %s => %s" args res))))

(defn scm! [cmd & args]
  (let [scm   (detect-scm)
        scm-cmd (get-in *scm-systems* [scm cmd])]
    (if-not scm-cmd
      (raise "No such SCM command: %s in %s" cmd scm))
    (apply sh! (concat scm-cmd args))))

(defn compute-next-development-version [current-version]
  (let [parts             (vec (.split current-version "\\."))
        version-parts     (vec (take (dec (count parts)) parts))
        minor-version     (last parts)
        new-minor-version (str (inc (Integer/parseInt minor-version)) "-SNAPSHOT")]
    (string/join "." (conj version-parts new-minor-version))))

(defn replace-project-version [old-vstring new-vstring]
  (let [proj-file     (slurp "project.clj")
        new-proj-file (.replaceAll proj-file (format "\\(defproject .+? %s" old-vstring) new-vstring )
        matcher       (.matcher
                       (Pattern/compile (format "(\\(defproject .+? )\"\\Q%s\\E\"" old-vstring))
                       proj-file)]
    (if-not (.find matcher)
      (raise "Error: unable to find version string %s in project.clj file!" old-vstring))
    (.replaceFirst matcher (format "%s\"%s\"" (.group matcher 1) new-vstring))))

(defn set-project-version! [old-vstring new-vstring]
  (spit "project.clj" (replace-project-version old-vstring new-vstring)))

(defn extract-project-version-from-file
  ([]
     (extract-project-version-from-file "project.clj"))
  ([proj-file]
     (let [s (slurp proj-file)
           m (.matcher (Pattern/compile "\\(defproject .+? \"([^\"]+?)\"") s)]
       (if-not (.find m)
         (raise "Error: unable to find project version in file: %s" proj-file))
       (.group m 1))))

(defn is-snapshot? [project]
  (.endsWith (:version project) "-SNAPSHOT"))

(defn get-current-version [project]
  (:version project))

(defn get-release-version [project]
  (.replaceAll (get-current-version project) "-SNAPSHOT" ""))

(defn update-project-map [project]
  (if (is-snapshot? project)
    (merge project
           {:version (get-release-version project)
            :original-version (get-current-version project)})
    project))

(defn update-project-file [project]
  (let [original-version (:original-version project)
        release-version  (:version project)]
    (println (format "setting project version %s => %s" original-version release-version))
    (set-project-version! original-version release-version)))

(defn tag [project]
  (let [release-version (:version project)]
    (println "adding, committing and tagging project.clj")
    (scm! :commit "-am" (format "lein-release plugin: preparing %s release" release-version))
    (scm! :tag (format "%s-%s" (:name project) release-version))))

(def predefined-cmds
  {"install" #(sh! "lein" "install")
   "clojars" #(sh! "scp" "pom.xml" jar-name (:clojars-url config))
   "deploy" #(sh! "lein" "deploy")
   "jar" #(sh! "lein" "jar")
   "pom" #(sh! "lein" "pom")
   "uberjar" #(sh! "lein" "uberjar")
   "embongo" #(sh! "lein" "embongo" "test")
   "rpm" #(sh! "lein" "rpm")})

(defn execute-task [task project]
  (prn (format "applying %s to project at version %s" task (:version project)))
  (if (vector? task)
    (prn "!!Vectored tasks not yet supported!!")
    (if-let [cmd (predefined-cmds (name task))]
      (cmd)
      (apply-task (name task) project nil))))

(defn execute-tasks [tasks project]  
  (doall
   (for [task tasks]
     (execute-task task project))))

(defn release [project-orig & args]  
  (let [project (update-project-map project-orig)
        release-version  (get-release-version project-orig)
        next-dev-version (compute-next-development-version release-version)]
    (binding [config (merge default-config (:lein-release project-orig))
              jar-name (format "target/%s-%s.jar" (:name project-orig) release-version)]
      (when (:original-version project)
        (update-project-file project)
        (tag project))
      (execute-tasks (:release-tasks config) project)
      (println (format "updating version %s => %s for next dev cycle" release-version next-dev-version))
      (set-project-version! release-version next-dev-version)
      (scm! :add "project.clj")
      (scm! :commit "-m"
            (format "lein-release plugin: bumped version from %s to %s for next development cycle"
                    release-version next-dev-version)))))

