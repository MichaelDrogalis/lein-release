(defproject lein-release/lein-release "1.0.60-SNAPSHOT"
  :description "Leiningen Release Plugin"
  :dev-dependencies [[swank-clojure "1.4.2"]]
  :eval-in-leiningen true
  :jar-name "aJar"
  :lein-release {:deploy-via :clojars
                 :release-tasks [:clean :install]}
  :dependencies [[org.clojure/clojure "1.3.0"]])