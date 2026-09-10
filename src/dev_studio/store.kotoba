(ns dev-studio.store
  "SSoT for the ISCO-08 2512 independent software-development-studio
  sole-proprietor actor, behind a `Store` protocol so the backend is a
  swap (MemStore default ‖ a real Datomic/kotoba-server backend, per the
  itonami actor pattern).

  Domain = independent software development studio:

    project              — a client project (projectId, name)
    build                — a build event under a project (buildId,
                           projectId, status #{:pass :fail})
    deploy               — a deployment event under a project (deployId,
                           projectId, target #{:staging :production})
    credential-rotation  — a credential rotation event (rotationId,
                           projectId)

  The append-only records are the operating ledger: a build, deploy or
  credential-rotation must reference a registered project, and these
  records are never mutated in place, only appended.")

(defprotocol Store
  (project [st project-id])
  (builds-of [st project-id])
  (deploys-of [st project-id])
  (credential-rotations-of [st project-id])
  (register-project! [st project])
  (record-build! [st build])
  (record-deploy! [st deploy])
  (record-credential-rotation! [st credential-rotation]))

(defrecord MemStore [state]
  Store
  (project [_ project-id]
    (get-in @state [:projects project-id]))
  (builds-of [_ project-id]
    (filter #(= project-id (:project-id %)) (:builds @state)))
  (deploys-of [_ project-id]
    (filter #(= project-id (:project-id %)) (:deploys @state)))
  (credential-rotations-of [_ project-id]
    (filter #(= project-id (:project-id %)) (:credential-rotations @state)))
  (register-project! [_ project]
    (swap! state assoc-in [:projects (:project-id project)] project))
  (record-build! [_ build]
    (swap! state update :builds (fnil conj []) build))
  (record-deploy! [_ deploy]
    (swap! state update :deploys (fnil conj []) deploy))
  (record-credential-rotation! [_ credential-rotation]
    (swap! state update :credential-rotations (fnil conj []) credential-rotation)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:projects {} :builds [] :deploys [] :credential-rotations []} seed)))))
