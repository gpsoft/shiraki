(defproject shiraki "0.1.1"
  :description "Image slideshow player"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.drewnoakes/metadata-extractor "2.18.0"]
                 [overtone/at-at "1.2.0"]]
  :main ^:skip-aot shiraki.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
