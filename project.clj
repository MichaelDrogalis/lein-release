(defproject lein-release/lein-release "1.0.56-SNAPSHOT"
  :description "Leiningen Release Plugin"
  :dev-dependencies [[swank-clojure "1.4.2"]]
  :eval-in-leiningen true
  :lein-release {:deploy-via :clojars
                 :release-tasks [:clean :install]}
  :dependencies [[org.clojure/clojure "1.3.0"]])