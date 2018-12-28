(defproject buildversion-maven-plugin/buildversion-maven-plugin "1.1-SNAPSHOT" 
  :compile-path "target/classes"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.cloudhoist/clojure-maven-mojo-annotations
                  "0.3.3"]
                 [org.cloudhoist/clojure-maven-mojo "0.3.3"]
                 [org.apache.maven/maven-plugin-api "3.5.4"]
                 [org.clojure/tools.trace "0.7.10"]]
  :source-paths ["src/main/clojure"]
  :jar-dir "target"
  :profiles {:dev {:dependencies [[radagast "1.1.0"]]}}
  :repositories {"sonatype-releases"
                 "https://oss.sonatype.org/content/repositories/releases"}
  :resource-paths ["src/main/resource"]
  :target-dir "target"
  :min-lein-version "2.0.0"
  :plugins [[lein-swank "1.4.4"] [lein-pprint "1.1.1"]]
  :test-paths ["src/test/clojure"]
  :warn-on-reflection true)
