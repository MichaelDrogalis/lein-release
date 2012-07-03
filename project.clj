(defproject lein-release/lein-release "1.0.47"
  :description "Leiningen Release Plugin"
  :dev-dependencies [[swank-clojure "1.4.2"]]
  :eval-in-leiningen true
  :lein-release {:deploy-via :clojars
                 :release-tasks [:clean :jar :pom :install]}
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :jvm-opts ["-server" "-Xms128M" "-Xmx256M"
             ;; Use these options in development to allow debugging 
             ;; with jswat on localhost port 9900 
             "-Xdebug"
             "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9900"])