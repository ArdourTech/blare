(defproject tech.ardour/blare "0.0.1"
  :description "Ardour Tech PubSub and EventBus Library"
  :url "https://github.com/ArdourTech/blare"
  :license {:name         "Eclipse Public License - v 1.0"
            :url          "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments     "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/core.async "1.3.610" :scope "provided"]
                 [tech.ardour/logging "0.0.1"]]
  :profiles {:test {:dependencies [[lambdaisland/kaocha "1.0.732"]]}}
  :source-paths ["src"]
  :test-paths ["test"]
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :sign-releases false}]]
  :aliases {"test" ["with-profile" "+test" "run" "-m" "kaocha.runner"]})
