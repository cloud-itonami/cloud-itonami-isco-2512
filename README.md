# cloud-itonami-isco-2512

Open Occupation Blueprint for **ISCO-08 2512**: Software Developers.

This repository designs a forkable OSS business for an independent software development studio (1 developer): a governor-gated actor proposes build/deploy actions, so the studio keeps its own delivery and audit records instead of renting a closed project-management SaaS. Physical-office robot tasks (hardware test-rig setup, workspace checks) follow the same robotics-premise gate as every other cloud-itonami-isco vertical.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a physical-office robot performs equipment setup, hardware testing rig assist and workspace checks (the software work itself stays human/LLM-assisted, gated the same way as every other proposal) under an actor that proposes
actions and an independent **Dev Studio Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
production deploys, data-schema migrations, or credential rotation) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client spec + design plan + test plan
        |
        v
Build Advisor -> Dev Studio Governor -> build/deploy, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2512`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/dev_studio/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `dev-studio.store` — `Store` protocol + `MemStore`: projects, builds,
  deploys, credential-rotation events. A build/deploy/credential-rotation
  can only be recorded against a registered project (project provenance).
- `dev-studio.governor` — `DevStudioGovernor`: `assess` gates a proposal
  against the project env. Hard invariants force `:hold` (no project,
  direct-write instead of `:propose`, or a production deploy below
  `:high` safety-class); production deploys always require `:high`+
  safety-class and thus `:human-approval`; credential-rotation proposals
  **always** escalate to `:human-approval` regardless of safety-class or
  confidence (no autonomous credential rotation); low-confidence
  proposals also escalate.

```bash
clojure -M:test   # 9 tests, 15 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the 15th `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112`, `-2221`, `-7126`, `-4321`, `-9312`, `-5322`,
`-8332`, `-1321`, `-3253`, `-6210`, `-5223`, `-7231`, `-8121` and `-9111`
(ADR-2607012000).

## License

AGPL-3.0-or-later.
