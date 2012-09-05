(ns leiningen.set-version-test
  (:use
   clojure.test
   [leiningen.set-version
    :only [default-version infer-previous-version update-version
           update-file-version]]))

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
                 "               [yyy \"0.1.0-SNAPSHOT\"]])"))))))

(deftest default-version-test
  (testing "default version"
    (is (= "0.2.0" (default-version {:version "0.2.0-SNAPSHOT"}))))
  (testing "no default version"
    (is (thrown? RuntimeException (default-version {:version "0.2.0"})))))

(deftest infer-previous-version-test
  (testing "infer from non snapshot version"
    (is (= "0.2.0" (infer-previous-version {:version "0.2.1"}))))
  (testing "infer from 2 component non snapshot version"
    (is (= "0.1" (infer-previous-version {:version "0.2"}))))
  (testing "infer from snapshot version"
    (is (= "0.2.0" (infer-previous-version {:version "0.2.1-SNAPSHOT"}))))
  (testing "no infer-previous version"
    (is (thrown? RuntimeException
                 (infer-previous-version {:version "0.2.0"})))))

(deftest update-file-version-test
  (testing "defaults"
    (is (= "some text referring to 0.2.1"
           (update-file-version
            {:name "xxx" :group "xxx" :version "0.2.1-SNAPSHOT"}
            "0.2.1"
            {}
            "some text referring to 0.2.1-SNAPSHOT"))))
  (testing "follow on text"
    (is (= "some text referring to 0.2.1 and ..."
           (update-file-version
            {:name "xxx" :group "xxx" :version "0.2.1-SNAPSHOT"}
            "0.2.1"
            {}
            "some text referring to 0.2.1-SNAPSHOT and ..."))))
  (testing "repeated text"
    (is (= "some text referring to 0.2.1 and referring to 0.2.1"
           (update-file-version
            {:name "xxx" :group "xxx" :version "0.2.1-SNAPSHOT"}
            "0.2.1"
            {}
            (str "some text referring to 0.2.1-SNAPSHOT "
                 "and referring to 0.2.1-SNAPSHOT")))))
  (testing "explicit search regex"
    (is (= "some text referring to 0.2.1"
           (update-file-version
            {:name "xxx" :group "xxx" :version "0.2.1-SNAPSHOT"}
            "0.2.1"
            {:search-regex #"referring to \d+\.\d+\.\d+(-SNAPSHOT)?"}
            "some text referring to 0.2.1-SNAPSHOT"))))
  (testing "explicit no-snapshot"
    (is (= "some text referring to 0.2.1"
           (update-file-version
            {:name "xxx" :group "xxx" :version "0.2.1-SNAPSHOT"}
            "0.2.1"
            {:search-regex #"referring to \d+\.\d+\.\d+(-SNAPSHOT)?"
             :no-snapshot true}
            "some text referring to 0.2.0")))))
