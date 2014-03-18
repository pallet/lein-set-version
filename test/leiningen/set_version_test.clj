(ns leiningen.set-version-test
  (:use
   clojure.test
   [leiningen.set-version
    :only [default-version next-version infer-previous-version kw-version
           update-version update-file-version version-components]]))

(deftest version-components-test
  (is (= {:base [1 0 1]
          :pre nil :pre-separator nil :pre-ver nil
          :snapshot false}
         (version-components "1.0.1")))
  (is (= {:base [1 0 1]
          :pre nil :pre-separator nil :pre-ver nil
          :snapshot true}
         (version-components "1.0.1-SNAPSHOT")))
  (is (= {:base [1 0 1]
          :pre "ALPHA" :pre-separator "" :pre-ver "1"
          :snapshot false}
         (version-components "1.0.1-ALPHA1")))
  (is (= {:base [1 0 1]
          :pre "BETA" :pre-separator "." :pre-ver "1"
          :snapshot false}
         (version-components "1.0.1-BETA.1")))
  (is (= {:base [1 0 1]
          :pre "RC" :pre-separator "." :pre-ver "1"
          :snapshot false}
         (version-components "1.0.1-RC.1"))))

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
  (testing "default version as snapshot"
    (is (= "0.2.0" (default-version {:version "0.2.0-SNAPSHOT"}))))
  (testing "default version, non-snapshot"
    (is (= "0.2.1-SNAPSHOT" (default-version {:version "0.2.0"})))))

(deftest next-version-test
  (is (= [1 3 6] (next-version [1 3 5] :point)))
  (is (= [1 4 0] (next-version [1 3 5] :minor)))
  (is (= [2 0 0] (next-version [1 3 5] :major))))

(deftest kw-version-test
  (testing "kw version as snapshot"
    (is (= "0.2.0" (kw-version {:version "0.2.0-SNAPSHOT"} :point)))
    (is (= "0.3.0" (kw-version {:version "0.2.1-SNAPSHOT"} :minor)))
    (is (= "1.0.0" (kw-version {:version "0.2.1-SNAPSHOT"} :major))))
  (testing "kw version, non-snapshot"
    (is (= "0.3.2-SNAPSHOT" (kw-version {:version "0.3.1"} :point)))
    (is (= "0.4.0-SNAPSHOT" (kw-version {:version "0.3.1"} :minor)))
    (is (= "1.0.0-SNAPSHOT" (kw-version {:version "0.3.1"} :major)))
    (testing "following pre-release"
      (is (= "0.3.1-SNAPSHOT"
             (kw-version {:version "0.3.1-alpha.1"} :point))))))

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
