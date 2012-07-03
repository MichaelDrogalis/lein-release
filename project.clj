(defproject lein-release/lein-release "1.0.63"
  :description "Leiningen Release Plugin"
  :dev-dependencies [[swank-clojure "1.4.2"]]
  :eval-in-leiningen true
  :lein-release {:release-tasks [:clean :clojars]
                 :clojars-url "http://clojars.mobile.lnx.nokia.com/"}
  :dependencies [[org.clojure/clojure "1.3.0"]])