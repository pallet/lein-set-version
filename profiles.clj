{:dev {:plugins [[lein-set-version "0.4.1"]]}
 :release
 {:set-version
  {:updates [{:path "README.md"
              :no-snapshot true
              :search-regex
              #"lein-set-version \"\d+\.\d+\.\d+\""}]}}}
