(ns dev-studio.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [dev-studio.store :as store]
            [dev-studio.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-project! st {:project-id "proj-1" :name "client-site"})
    st))

(deftest proceeds-on-clean-build
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :build :project-id "proj-1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-project
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :build :project-id "no-such-project" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-project (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :build :project-id "proj-1" :safety-class :low
                   :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest proceeds-on-staging-deploy
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :deploy :target :staging :project-id "proj-1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-production-deploy-without-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :deploy :target :production :project-id "proj-1" :safety-class :medium
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :production-deploy-safety (:rule %)) (:violations result)))))

(deftest human-approval-on-production-deploy-with-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :deploy :target :production :project-id "proj-1" :safety-class :high
                   :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest credential-rotation-always-escalates
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :credential-rotation :project-id "proj-1" :safety-class :none
                   :effect :propose :confidence 1.0}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :credential-rotation (:reason result)))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :build :project-id "proj-1" :safety-class :none
                   :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-build! st {:build-id "b1" :project-id "proj-1" :status :pass})
    (store/record-deploy! st {:deploy-id "d1" :project-id "proj-1" :target :staging})
    (is (= 1 (count (store/builds-of st "proj-1"))))
    (is (= 1 (count (store/deploys-of st "proj-1"))))))
