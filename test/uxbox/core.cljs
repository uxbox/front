(ns uxbox.core-test
  (:require [cljs.test :as t]))

(t/deftest a-passing-test
  (t/testing "That passes"
    (t/is (= 42 42))))
