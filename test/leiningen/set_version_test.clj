(ns leiningen.set-version-test
  (:use
   clojure.test
   [leiningen.set-version :only [update-version]]))

(deftest version-test
  (testing "simple case"
    (is (= "(defproject xxx \"0.2.0\"\n:description \"me\")"
           (update-version
            {:name "xxx" :group "xxx" :version "0.1.0"} "0.2.0"
            "(defproject xxx \"0.1.0\"\n:description \"me\")"))))
  (testing "group"
    (is (= "(defproject abc/xxx \"0.2.0\"\n:description \"me\")"
           (update-version
            {:name "xxx" :group "abc" :version "0.1.0"} "0.2.0"
            "(defproject abc/xxx \"0.1.0\"\n:description \"me\")"))))
  (testing "snapshot"
    (is (= "(defproject xxx \"0.2.0\"\n:description \"me\")"
           (update-version
            {:name "xxx" :group "xxx" :version "0.1.0-SNAPSHOT"} "0.2.0"
            "(defproject xxx \"0.1.0-SNAPSHOT\"\n:description \"me\")"))))
  (testing "multiline"
    (is (= "(defproject xxx\n \"0.2.0\"\n:description \"me\")"
           (update-version
            {:name "xxx" :group "xxx" :version "0.1.0-SNAPSHOT"} "0.2.0"
            "(defproject xxx\n \"0.1.0-SNAPSHOT\"\n:description \"me\")"))))
  (testing "extra spaces"
    (is (= "(defproject \txxx\n  \"0.2.0\" \n :description \"me\")"
           (update-version
            {:name "xxx" :group "xxx" :version "0.1.0-SNAPSHOT"}
            "0.2.0"
            "(defproject \txxx\n  \"0.1.0-SNAPSHOT\" \n :description \"me\")"))))
  (testing "dependencies"
    (is (= (str "(defproject yyy \"0.2.0\"\n:description \"me\" "
                ":dependencies [[xxx \"0.2.0\"]])")
           (update-version
            {:name "yyy" :group "yyy" :version "0.1.0-SNAPSHOT"}
            "0.2.0"
            (str "(defproject yyy \"0.1.0-SNAPSHOT\"\n:description \"me\" "
                 ":dependencies [[xxx \"0.1.0-SNAPSHOT\"]])")))))
  (testing "multiple dependencies"
    (is (= (str "(defproject zzz \"0.2.0\"\n:description \"me\" "
                ":dependencies [[xxx \"0.2.0\"]\n"
                "               [yyy \"0.2.0\"]])")
           (update-version
            {:name "zzz" :group "zzz" :version "0.1.0-SNAPSHOT"}
            "0.2.0"
            (str "(defproject zzz \"0.1.0-SNAPSHOT\"\n:description \"me\" "
                 ":dependencies [[xxx \"0.1.0-SNAPSHOT\"]\n"
                 "               [yyy \"0.1.0-SNAPSHOT\"]])")))))
  (testing "default version"
    (is (= "(defproject xxx \"0.2.0\"\n:description \"me\")"
           (update-version
            {:name "xxx" :group "xxx" :version "0.2.0-SNAPSHOT"} nil
            "(defproject xxx \"0.2.0-SNAPSHOT\"\n:description \"me\")"))))
  (testing "no default version"
    (is (thrown? RuntimeException
           (update-version
            {:name "xxx" :group "xxx" :version "0.2.0"} nil
            "(defproject xxx \"0.2.0\"\n:description \"me\")")))))
