---
name: prepare-release
description: Prepare a regular X.Y release end to end — checks the previous release is merged back into develop (stops if not), runs "[RM] Prepare release" in tangem-sdk-android and blockchain-sdk-kotlin, publishes their GitHub Packages artifacts from the new release branches, runs "[RM] Prepare release" in the app repo, then opens a PR into the app's releases/X.Y that pins the release SDK builds in tangem_dependencies.toml and sets the given feature toggles to X.Y. Use when the user asks to "prepare the release", "подготовь релиз", "срежь релиз X.Y", "/prepare-release 6.10".
allowed-tools: Read, Bash, AskUserQuestion, Monitor, TaskStop
argument-hint: <X.Y> [TOGGLE_NAME | TOGGLE_NAME=version ...]
---

Cut release `X.Y` across the Card SDK, Blockchain SDK and the app, then pin the release SDK builds and
release toggles on the app's release branch.

Every step that dispatches a workflow is **outward-facing** (creates release branches, renames Jira
versions, publishes packages). Run them only after the confirmation gate in Phase 2.

## Constants

| | Repo (`tangem-developments/…`) | Prepare | Publish | Dependency key |
|---|---|---|---|---|
| Card SDK | `tangem-sdk-android` | `prepare_release.yml` | `publish_artifact.yml` | `tangemCardSdk` |
| Blockchain SDK | `blockchain-sdk-kotlin` | `prepare_release.yml` | `publish_artifact.yml` | `tangemBlockchainSdk` |
| App | `tangem-app-android` | `prepare_release.yml` | — | — |

- Prepare workflows are dispatched on `develop` with input `version_name=X.Y`. They create
  `releases/X.Y` from develop's head. The SDK prepare accepts **only `X.Y`** — hotfixes (`X.Y.Z`) are
  out of scope for this skill.
- Publish is dispatched on `releases/X.Y`. It tags the commit `releases/X.Y-<N>` and publishes the
  artifact as version **`releases-X.Y-<N>`** (slash → dash). That string goes into
  `gradle/tangem_dependencies.toml`.
- `tangemHotSdk`, `tangemVico`, `tangemUsedeskSdk` are **not** touched.
- Scripts: `.claude/skills/prepare-release/scripts/` (`SCRIPTS` below). Run them from the repo root:
  `preflight.sh`, `prepare_status.sh`, `publish_status.sh`, `dispatch.sh`, `set_toggles.py`,
  `pin_release.sh`.
- **Bash calls share no shell variables.** Carry run ids, versions and paths between calls as literal
  values in the next command — never `$RUN_ID` set in an earlier call.
- Every skip/resume decision comes from the real state the status scripts report (active runs, job
  results, tag vs. branch head) — never from a branch or tag merely existing.

## Phase 0 — Parse arguments

- First argument: version. Must match `^[0-9]+\.[0-9]+$`; otherwise stop and say why.
- The rest: the toggles that ship enabled in this release — whitespace/comma/newline separated.
  `NAME` → version `X.Y`; `NAME=version` → exactly that version, applied as is (`X.Y[.Z]` or
  `undefined`; anything else is rejected). Only the listed toggles are touched; every other toggle
  keeps its version. A plain `NAME` already enabled in an earlier release (e.g. `6.8`) is kept as is,
  not bumped to `X.Y`.
- No toggles given → ask once whether the release ships without toggle changes; an empty list is valid.
- `gh auth status` must show the `workflow` scope; otherwise stop and suggest
  `! gh auth refresh -s workflow`.

## Phase 1 — Preflight (stop on any blocker)

```bash
bash SCRIPTS/preflight.sh X.Y
git fetch origin develop
python3 SCRIPTS/set_toggles.py --check --ref origin/develop X.Y <toggles...>
```

`--ref origin/develop` validates against what the release branch will be cut from, not the user's
(possibly dirty or stale) checkout. On resume, when the app's `releases/X.Y` already exists, use
`--ref origin/releases/X.Y` after `git fetch origin releases/X.Y`.

`preflight.sh` checks, in all three repos, the latest numeric `releases/*` branch other than
`releases/X.Y`:
- it has no commits that aren't in `develop` (`compare develop...<last>` → `ahead_by == 0`);
- there is no open PR into `develop` from `releases/*` or `merge/releases/*`.

**Exit 1 → stop.** Show the blocker lines (repo, branch / PR link) and tell the user the previous
release must be merged into develop first. Do not dispatch anything.

Also stop if `set_toggles.py --check` reports unknown toggles — list them.

`INFO releases/X.Y already exists` is not a blocker: the run is being resumed, and the phases below
skip what is already done for that repo.

## Phase 2 — Confirm

Show one plan and ask with `AskUserQuestion` (Proceed / Cancel):

- version `X.Y`;
- per repo: the `prepare_status.sh` / `publish_status.sh` state and what will happen;
- toggles and their planned changes (output of `--check`);
- the final PR: `tech/X.Y_enable_toggles_sdk` → `releases/X.Y` in the app repo.

Nothing below runs without "Proceed".

## Phase 3 — Prepare both SDKs (in parallel)

For each SDK repo: `bash SCRIPTS/prepare_status.sh tangem-developments/<repo> X.Y`

| State | Action |
|---|---|
| `absent` | `bash SCRIPTS/dispatch.sh tangem-developments/<repo> prepare_release.yml develop version_name=X.Y` → prints the run id |
| `running <id>` | don't dispatch — wait for `<id>` (a second run would cut the branch from a newer develop) |
| `done <id>` | nothing to do |
| `partial <id>` | the branch exists but a job failed — stop, show `gh run view <id> -R … --log-failed \| tail -40` |

`dispatch.sh` also refuses to start a second run while one is active on the same ref and returns
that run's id instead.

Print each run's URL (`https://github.com/tangem-developments/<repo>/actions/runs/<id>`). Wait for
all of them with **one background Bash call** (`run_in_background: true`), literal ids filled in:

```bash
gh run watch <id1> -R tangem-developments/tangem-sdk-android --exit-status --interval 15 > /dev/null; e1=$?
gh run watch <id2> -R tangem-developments/blockchain-sdk-kotlin --exit-status --interval 15 > /dev/null; e2=$?
echo "card=$e1 blockchain=$e2"; exit $((e1 | e2))
```

The prepare job runs in the `Create Release` environment. If a run sits in `waiting` for a few
minutes, tell the user it needs approval in the Actions UI (link) and keep waiting.

A failed run → stop with the failed log tail. Afterwards `prepare_status.sh` must report `done` for
both repos; anything else → stop.

## Phase 4 — Publish SDK artifacts from the release branches (in parallel)

For each SDK repo: `bash SCRIPTS/publish_status.sh tangem-developments/<repo> X.Y`. A non-zero exit is
an API/auth failure → stop; never treat it as `absent`.

| State | Action |
|---|---|
| `absent` | no tag at the branch head (none yet, or the branch got commits after the last publish) → `bash SCRIPTS/dispatch.sh tangem-developments/<repo> publish_artifact.yml releases/X.Y` |
| `running <id>` | wait for `<id>` (e.g. the auto-publish of a cherry-picked fix) |
| `done releases-X.Y-N` | that is the artifact version |
| `no-branch` | `releases/X.Y` is missing — expected only in the Phase 2 plan (“publish after prepare”); here, after Phase 3, stop |

Wait for dispatched/running runs as in Phase 3, then call `publish_status.sh` again: it must report
`done releases-X.Y-N` for both repos. Anything else after a successful run → stop and report; never
guess the number from the run number. The `done` value goes into `tangem_dependencies.toml` as is.

## Phase 5 — Prepare the app

`bash SCRIPTS/prepare_status.sh tangem-developments/tangem-app-android X.Y`, then act as in Phase 3
(`absent` → dispatch `prepare_release.yml` on `develop` with `version_name=X.Y`; `running` → wait).
Wait in the background, then check the status again — it must be `done`.

`partial <id>` here usually means the **Jira version rename** (`Prepare Jira version (AND/CRASHAND)`
jobs) failed while the branch was created. Show which jobs failed and ask the user (Rerun failed jobs /
Stop). On rerun: `gh run rerun <id> -R tangem-developments/tangem-app-android --failed`, wait for it,
check the status again. Never report success while the status is `partial`.

## Phase 6 — Pin SDK builds and toggles on the release branch

One script, one Bash call — it never touches the user's checkout (all edits happen in a throwaway
worktree) and resumes safely:

```bash
CO_AUTHOR="Claude <model>" bash SCRIPTS/pin_release.sh X.Y <card version> <blockchain version> <scratchpad>/release-X.Y <toggles...>
```

`<model>` is the model running the session (take it from the harness's commit-trailer instruction).
The versions are the literal `done` values from Phase 4.

What it does:
- a PR `tech/X.Y_enable_toggles_sdk` → `releases/X.Y` already exists (any state) → prints its URL;
- the branch is on origin without a PR → opens the PR;
- otherwise creates the worktree from `origin/releases/X.Y`, sets both SDK versions and the toggles,
  refuses any change outside `tangem_dependencies.toml` / `feature_toggles.json`, commits
  (`Enable X.Y feature toggles and set release SDK builds`, technical → no `AND-xxx`), pushes and opens
  the PR (label `easy`, one-sentence body + attribution footer);
- a local branch with that name is deleted only if it is a leftover of a failed run of this script;
  anything else → it stops without touching it;
- `PIN_DRY_RUN=1` stops after printing the diff.

The last stdout line is the PR URL. Show the diff summary it printed.

## Phase 7 — Report

A short summary:
- release branches: links to `releases/X.Y` in the three repos, with the final `prepare_status.sh` state
  of each (`done` only);
- artifacts: `tangemCardSdk = releases-X.Y-N`, `tangemBlockchainSdk = releases-X.Y-M`;
- toggles set (name → version);
- the PR link.

## Rules

- Never dispatch a workflow before the Phase 2 confirmation; never push to `releases/X.Y` directly —
  always the PR.
- A failed or blocked step stops the skill; rerunning it with the same arguments resumes from the
  state the status scripts report (active runs are awaited, not duplicated).
- Never poll with `sleep` loops in the foreground — wait with `gh run watch` in a background Bash call.