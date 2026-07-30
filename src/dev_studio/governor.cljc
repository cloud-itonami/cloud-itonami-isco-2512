(ns dev-studio.governor
  "DevStudioGovernor — the independent safety/traceability layer for the
  ISCO-08 2512 independent software-development-studio actor. The Build
  Advisor proposes actions (build, deploy, credential-rotation); it has
  no notion of project provenance or production-deploy risk, so this MUST
  be a separate system able to *reject* a proposal and fall back to HOLD
  — the itonami-actor pattern (independent Governor gates a proposing
  actor) applied to this occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. A production deploy or a
  credential rotation ALWAYS requires human sign-off — neither can ever
  be auto-approved.

  HARD invariants for :dev-studio/propose:
    1. Project provenance — a build, deploy or credential-rotation must
       reference a registered project.
    2. No-actuation       — the proposal must not directly mutate a
       build/deploy/credential-rotation record outside the
       record-build!/record-deploy!/record-credential-rotation! path
       (effect must be :propose, never a raw store write).
    3. Production-deploy safety — a deploy with `target :production`
       always requires :high or higher safety-class, forcing human
       sign-off; it is never auto-approved regardless of confidence.
  SOFT:
    4. Credential rotation always escalates to human sign-off (no
       autonomous credential rotation).
    5. Confidence floor → escalate."
  (:require [dev-studio.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- hard-violations [{:keys [project-fn]} proposal]
  (let [{:keys [kind target project-id safety-class effect]} proposal
        found-project (project-fn project-id)]
    (cond-> []
      (nil? found-project)
      (conj {:rule :no-project :detail (str "未登録 project " project-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and (= kind :deploy) (= target :production)
           (< (safety-rank (or safety-class :none)) (safety-rank :high)))
      (conj {:rule :production-deploy-safety
             :detail "production deploy は :high 以上の safety-class が必須"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:project-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 0.0)
        credential-rotation? (= :credential-rotation (:kind proposal))]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      credential-rotation?
      {:decision :human-approval :violations [] :confidence confidence
       :reason :credential-rotation}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `dev-studio.store/Store` implementation."
  [store]
  {:project-fn #(store/project store %)})
