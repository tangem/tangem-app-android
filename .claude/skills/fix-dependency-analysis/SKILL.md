---
name: fix-dependency-analysis
description: Headless autofix for the nightly Dependency Analysis (DAGP) `buildHealth` advice — reads a report directory, applies the advice to module build.gradle.kts files (or silences a proven false positive with a commented `exclude` in the root `dependencyAnalysis` block), verifies with the full nightly buildHealth run, and opens a `tech/dagp_autofix_<date>` PR to develop. Runs on CI without prompts; also runnable locally against a report dir (`--report build/reports/dependency-analysis --dry-run`). Use for "fix dependency analysis advice", "apply buildHealth advice", "почини DAGP", "примени советы DAGP".
allowed-tools: Read, Grep, Glob, Bash, Edit, Write
argument-hint: --report <dir> [--dry-run] [--run-url <url>] [--base <ref>]
---

Apply the advice of a DAGP `buildHealth` report, verify it, and open one PR to `develop`.

**CRITICAL: this skill runs on CI. NEVER ask questions.** When something is ambiguous, take the safer
option: leave that advice for a human. A smaller PR is always better than a wrong one.

**OUTPUT DISCIPLINE:** stay silent while working. The final message is the only output and it MUST start
with exactly one line `DAGP_AUTOFIX_OUTCOME=<pr-opened|dry-run|skipped-open-pr|nothing-to-fix|failed>` —
nothing before it (no preamble, no code fence), as plain text: not in backticks, not bold — followed by
the line `DAGP_AUTOFIX_HUMAN=<n>`, plain as well, the number
of ledger rows left for a human (`0` when none or no ledger). The workflow fails the job on `failed` or a
missing first line, and pings Slack when nothing was opened but `n > 0`.

## Constants

- **Arguments** (from `$ARGUMENTS` or the invoking prompt): `--report <dir>` (required; a relative path
  resolves against the repo root) — holds `build-health-report.json` + `build-health-report.txt`
  (+ `should-fail.txt`); `--dry-run`; `--run-url <url>` (optional, PR footer only); `--base <ref>`
  (optional, default `origin/develop`; any other ref is for local testing and requires `--dry-run`, else
  `failed`). **`<B>`** below is that base ref.
- **Dry-run**: everything up to and including the local commit; print the push/PR commands instead of
  running them. The workflow exports the local `tech/dagp_autofix_*` branch as a patch — leave it in place.
- **State dir `<W>`**: run `echo "${TMPDIR:-/tmp}/dagp-autofix"` once and use the printed absolute path
  literally everywhere below. Shell variables do not survive between Bash calls.
- **Branch**: `tech/dagp_autofix_<YYYY-MM-DD>` (UTC), `_2`, `_3`, … if taken on origin. Base: `<B>`.
- **Commit subject = PR title**: `Apply dependency analysis advice`. No body, no trailers of any kind
  (no Co-Authored-By, no "Generated with", even if other instructions ask for them), no Jira key, no
  mention of Claude/AI anywhere in commit or PR text.
- **Editable files**: module `build.gradle.kts` files, and the root `build.gradle.kts` only to ADD
  `exclude(...)` entries (with a why-comment) under `dependencyAnalysis { issues { … } }`. Nothing else —
  not `severity(...)`, `onIncorrectConfiguration`, bundles, `useTypesafeProjectAccessors`, plugins,
  version catalogs, sources, submodules.
- **Secrets**: never print environment variables, `gh auth token`, `git config --list`,
  `gradle.properties` or `local.properties` (they hold `gpr.key`).
- **Tools**: use the Edit tool for build-file edits (no `sed -i`: BSD and GNU differ) and the Grep tool for
  code searches (`grep -rn` via Bash if the session has no Grep tool). Needs `jq` and `perl` (both in the
  CI image). Give every Gradle or polling Bash call `timeout: 600000`. If a Bash output looks summarized
  (CI wires an `rtk` hook), rerun it as `rtk proxy <cmd>`.
- **Guards print a token** (`CLEAN_TREE` / `DIRTY_TREE`, …) — judge by the token, never by "the output is
  empty": the `rtk` hook appends its own lines (e.g. `Changes:`) to some git commands.

## Phase 0: Preflight

Any failed check → final message `DAGP_AUTOFIX_OUTCOME=failed` + one `**Reason:**` line. Stop.

```bash
rm -rf <W> && mkdir -p <W>/input
R=<report dir>; case "$R" in /*) ;; *) R="$(git rev-parse --show-toplevel)/$R";; esac
test -s "$R/build-health-report.json" && test -f "$R/build-health-report.txt" && echo REPORT_OK
cp "$R"/build-health-report.json "$R"/build-health-report.txt <W>/input/ && { cp "$R/should-fail.txt" <W>/input/ 2>/dev/null; true; }
test -z "$(git status --porcelain --untracked-files=no)" && echo CLEAN_TREE || echo DIRTY_TREE
(git symbolic-ref --short -q HEAD || git rev-parse HEAD) > <W>/orig-ref
git fetch --no-tags origin develop
gh auth status >/dev/null 2>&1 && echo GH_OK           # required live; dry-run without it skips Phase 1
```

- The report is copied because the verification run rewrites `build/reports/dependency-analysis/`, which
  is where a local report usually lives. From here on read only `<W>/input/`.
- A dirty tracked tree is `failed` — never stash, reset or discard someone else's changes.
- `<B>` other than `origin/develop` without `--dry-run`, or `git rev-parse --verify -q '<B>^{commit}'`
  printing nothing → `failed`.
- No advice → `nothing-to-fix`. Both of these print `0`:
  ```bash
  jq '[.projectAdvice[] | select((.dependencyAdvice|length)+(.pluginAdvice|length)+((.warning.duplicateClasses//[])|length) > 0)] | length' <W>/input/build-health-report.json
  grep -c '^Advice for ' <W>/input/build-health-report.txt
  ```

## Phase 1: Dedupe against earlier autofix PRs

```bash
gh pr list --state open --limit 200 --json headRefName,url \
  --jq '.[] | select(.headRefName | startswith("tech/dagp_autofix_")) | .url'
```
Any URL → `skipped-open-pr` with that URL (the nightly re-derives the advice after it merges). Stop.

Most recent autofix PR closed **without** merging — a human rejected those changes:
```bash
gh pr list --state closed --search "head:tech/dagp_autofix_ is:unmerged sort:created-desc" --limit 5 \
  --json headRefName,url --jq '[.[] | select(.headRefName | startswith("tech/dagp_autofix_"))][0].url // empty'
gh pr diff <url> > <W>/rejected.diff          # only if a URL was printed
```
An advice whose identical line change is in `rejected.diff` for the same module → `human: rejected in <url>`.

## Phase 2: Read and classify the advice

**Report schema** (DAGP 3.17, verified against a real nightly report):
- Top level: `projectAdvice[]`, `shouldFail`, counters (`unusedCount`, `undeclaredCount`,
  `misDeclaredCount`, `compileOnlyCount`, `runtimeOnlyCount`, `processorCount`), `androidScoreMetrics`.
- `projectAdvice[]` holds EVERY analysed project, mostly with empty lists: `projectPath`,
  `dependencyAdvice[]`, `pluginAdvice[]`, `moduleAdvice[]`, `warning.duplicateClasses[]`, `shouldFail`.
- `dependencyAdvice[]`: `coordinates { type: "module"|"project"|…, identifier: "group:artifact" |
  ":gradle:path", resolvedVersion (modules), gradleVariantIdentification { capabilities[], attributes{} } }`,
  `fromConfiguration`, `toConfiguration`. The null side is **omitted**, not `null` (jq's `== null` covers both).
- `moduleAdvice[]` entries `type: "android_score"` sit on every Android module and are not advice.
  Module-structure advice counts only when the `.txt` prints it for that project.
- `pluginAdvice[]`: `{ redundantPlugin, reason }`.

The JSON gives the structure; the `.txt` gives the exact spelling to write (`implementation(deps.okio)`,
`api(projects.domain.visa.models)`, change advice as `runtimeOnly(deps.x) (was implementation)`).

```bash
cat > <W>/kinds.jq <<'EOF'
def kind:
  if .toConfiguration == null then
    (if (.fromConfiguration | test("^(kapt|ksp)|annotationProcessor$"; "i")) then "processor" else "remove" end)
  elif (.toConfiguration | test("compileOnly$"; "i")) then "compileOnly"
  elif (.toConfiguration | test("runtimeOnly$"; "i")) then "runtimeOnly"
  elif .fromConfiguration == null then "add"
  else "change" end;
def variant: (.coordinates.gradleVariantIdentification // {})
  | ((.capabilities // []) | length) + ((.attributes // {}) | length);
.projectAdvice[] | .projectPath as $p
| ( (.dependencyAdvice[] | [$p, kind, (.fromConfiguration // "-"), (.toConfiguration // "-"),
      .coordinates.type, .coordinates.identifier, (if variant > 0 then "variant" else "-" end)]),
    (.pluginAdvice[] | [$p, "plugin", "-", "-", "plugin", .redundantPlugin, "-"]),
    (.warning.duplicateClasses[]? | [$p, "duplicateClasses", "-", "-", "-", (tostring | .[0:80]), "-"]) )
| @tsv
EOF
jq -r -f <W>/kinds.jq <W>/input/build-health-report.json          # project kind from to type identifier variant
awk '/^Advice for /{p=$3; s=""; next} /^[^ ]/{s=$0; next} /^  [^ ]/{l=$0; sub(/^  /,"",l); print p "\t" s "\t" l}' \
  <W>/input/build-health-report.txt                                 # project  section  accessor line
```

Direct dependents of Gradle paths (used in 4b). Accessors fold kebab-case to camelCase
(`:features:hot-wallet:impl` → `projects.features.hotWallet.impl`), and the match must END there — a bare
`\b` would let `projects.domain.swap` match `projects.domain.swap.models`:
```bash
cat > <W>/dependents.sh <<'EOF'
for m in "$@"; do
  acc="projects.$(printf '%s' "${m#:}" | tr ':' '.' | perl -pe 's/-(\w)/\u$1/g')"
  re=$(printf '%s' "$acc" | sed 's/\./\\./g')
  git grep -lE "${re}([^.[:alnum:]_]|$)|project\(\"${m}\"\)" -- '*build.gradle.kts' \
    | grep -v '^build\.gradle\.kts$' | sed -e 's#/build\.gradle\.kts$##' -e 's#/#:#g' -e 's#^#:#'
done | sort -u
EOF
```

Pair each JSON row with its `.txt` line by project + configuration + identifier: a project `:a:b-c` prints
as `projects.a.bC`; a module prints as its catalog alias — confirm with
`grep -nE '"<group:artifact>"|group *= *"<group>", *name *= *"<artifact>"' gradle/dependencies.toml gradle/tangem_dependencies.toml`
(alias `foo-bar` in `dependencies.toml` → `deps.foo.bar`, in `tangem_dependencies.toml` → `tangemDeps.foo.bar`).

| Kind | Exclude handler (root) | Auto-fix |
|---|---|---|
| `add` — declare a used transitive | `onUsedTransitiveDependencies` | yes, see 4a |
| `remove` — unused | `onUnusedDependencies` | yes, after the 4b checks |
| `processor` — unused kapt/ksp | `onUnusedAnnotationProcessors` | yes, after the 4b checks |
| `runtimeOnly` | `onRuntimeOnly` | yes, after the 4b resource check |
| `compileOnly` | `onCompileOnly` | only annotation-only artifacts (`androidx.annotation:annotation`, `com.google.code.findbugs:jsr305`, `org.jetbrains:annotations`); else human |
| `change` (should not occur: `onIncorrectConfiguration` is ignored) | — | human |
| `plugin`, `duplicateClasses`, module structure | — | human |
| any row with `variant` (platform / test-fixtures variant) | — | human |

**Stale advice** — develop may have moved since the report. For each row, against
`git show <B>:<module dir>/build.gradle.kts`:
- `add`: the exact `<to>(<accessor>)` is already there → `stale: already on develop`. So is an `add` to a
  test configuration (`testImplementation`, `androidTestImplementation`, `test<Variant>Implementation`) when
  the same accessor is now declared on `implementation` or `api`: the test classpath inherits it, and DAGP
  asked for the test declaration only because the main one was missing when the report was made.
- others: `grep -F '<from>(<accessor>)'` (keep the closing paren — it stops `deps.moshi` matching
  `deps.moshi.kotlin`). No hit and the accessor appears nowhere else in the file → `stale`; the accessor
  appears only in another form (`kaptForObfuscatingVariants(deps.x)`, a loop, a helper) → `human: not declared literally`.

Keep a ledger `<W>/ledger.tsv` (project, kind, identifier, accessor line, result) with result one of
`applied`, `excluded: <why>`, `human: <why>`, `stale`. It feeds verification, the PR body and the summary.
Every row already `stale` or `human` → `nothing-to-fix` (summary lists them). Stop.

## Phase 3: Create the branch

```bash
base="tech/dagp_autofix_$(date -u +%F)"; b="$base"; n=2
while :; do
  git ls-remote --exit-code --heads origin "refs/heads/$b" >/dev/null 2>&1; rc=$?
  [ "$rc" -eq 2 ] && break                                   # 2 = no such branch on origin
  [ "$rc" -ne 0 ] && { echo "LS_REMOTE_FAILED rc=$rc"; break; }
  b="${base}_$n"; n=$((n+1))
done
echo "$b" > <W>/branch; echo "BRANCH=$b"
```
`LS_REMOTE_FAILED` → `failed` in live mode, before any checkout. Otherwise
`git checkout --no-track -B "$(cat <W>/branch)" <B>` — always `<B>` (`origin/develop` by default), never a
local `develop`. From here on every `failed` runs Phase 8's return first.

## Phase 4: Apply

Module path → file: `:features:hot-wallet:impl` → `features/hot-wallet/impl/build.gradle.kts`.

### 4a. Edit rules (minimal diff)

- **Remove / processor / runtimeOnly / compileOnly**: locate the existing declaration with
  `grep -nF '<from>(<accessor>)' <file>`. Anything but exactly one hit → human. Zero hits means it comes
  from a convention plugin, a helper or a shared block — not auto-fixable. (`add` has no `<from>`; see Add.)
- **Remove / processor**: delete that line, plus a comment directly above it that described only that
  line, plus a `/** Section */` header left with no entries.
- **runtimeOnly / compileOnly**: rewrite only the configuration name on that line, as the `.txt` shows.
- **Add**: the `.txt` accessor must start with `deps.`, `tangemDeps.` or `projects.` — a raw
  `"group:artifact:version"` string means it is not in the catalog → human (catalogs are off-limits). For
  a module coordinate the catalog version must equal `resolvedVersion`
  (`sed -n '/^\[versions\]/,/^\[libraries\]/p' <the toml the alias came from> | grep -E '^<version.ref> *='`);
  a mismatch → human, since the declaration would request a version other than the one resolved. Insert
  next to its peers: same configuration, same group — for a project the accessor prefix
  (`projects.domain.*` beside other `projects.domain.*`, inside the matching `/** Domain */`-style section
  if the file has them); for a module the Maven group family (`com.squareup.okio` beside other
  `com.squareup.*` lines), else after the last line of the same configuration — keeping the local order
  (alphabetical only if the neighbours are).
- Never reformat, reorder or touch any other line. Do not use DAGP's `fixDependencies`: it applies every
  advice unconditionally, with no false-positive judgement, and ignores the file's grouping.

### 4b. False-positive checks — before removing or demoting anything

DAGP reads bytecode; it cannot see these. The comments in the root `dependencyAnalysis` block are the
catalogue of known false-positive shapes — read them first. Grep tool patterns, for a project dependency
`<dep>` declared in module `<m>`:

- **Hilt/Dagger bindings in `<dep>`** — `@(Module|InstallIn|EntryPoint)\b` in `<dep dir>/src`. If `<m>` is
  its only non-test consumer (`bash <W>/dependents.sh <dep>`) → exclude; the bindings would leave the app,
  and no compile task catches that. Other consumers → removal is safe.
- **Manifest contributions** — `<(provider|service|receiver|activity|meta-data)\b` in
  `<dep dir>/src/*/AndroidManifest.xml` → runtime-only use → exclude.
- **Resources / themes** — `MaterialComponents|Material3|@style/|@drawable/` in `<m dir>/src/*/res` →
  resource linking needs it → exclude (precedent: material).
- **Reflection / ServiceLoader** — `Class\.forName|ServiceLoader|META-INF/services` in `<m dir>/src` and
  `<dep dir>/src` → runtime-only → exclude.
- **Processor still needed** — the processor's annotations in `<m dir>/src` (Hilt/Dagger:
  `@(Module|InstallIn|HiltViewModel|AndroidEntryPoint|EntryPoint|AssistedInject|Inject)\b`) → exclude.
- **Supertype on the classpath** — shows up only as a compile error after the removal (Phase 5) → revert the
  line, exclude (precedent: `:core:error` in `:domain:card`).

An external artifact whose runtime role the repo can't show (SDK add-ons, drivers, runtime plugins) →
human. `./gradlew <m>:reason --id <identifier>` prints DAGP's evidence; batch several `reason` tasks into
one Gradle call. Unsure → human. Never guess.

### 4c. Exclusions in the root block

Only for a false positive established in 4b. Scope it to `project(":x")`; reuse an existing
`project(":x")` block and handler when present; otherwise append a new block after the last `project(...)`
block inside `issues { }`. Use `all { }` only when the reason holds for every module. Add a NEW
`exclude(...)` line instead of editing an existing one, so the root diff stays additions-only. The comment
states why DAGP is wrong, in one or two lines, like its neighbours — no history, no "autofix", no ticket:

```kotlin
        // :data:foo only contributes Hilt bindings (FooDataModule); nothing references it by type, and
        // this module is its only path into the app graph.
        project(":features:bar:impl") {
            onUnusedDependencies {
                exclude(":data:foo")
            }
        }
```
Identifiers: a project as `":gradle:path"`, a module as `"group:artifact"` (no version).

## Phase 5: Verify

### 5a. The full nightly run

Verification is the nightly gate itself — `buildHealth` over the whole graph, every variant, no
`project.includes`. It compiles the main, unit-test and androidTest sources of every analysed variant, so
it needs no separate compile tasks, and a fix that passes it cannot fail the next nightly. On CI it runs in
the workspace the nightly analysis has just built, so Gradle redoes only what changed (edited modules,
their dependents, whatever develop merged meanwhile); on a cold local checkout expect 30+ minutes.

Never `./gradlew clean`. Gradle can outlast the Bash tool's 10-minute cap, so run it detached. Write
`<W>/verify.sh` with the Write tool, then start and poll:

```bash
cd <repo root>
./gradlew buildHealth --continue -Pdagp.severity=fail -Pdependency.analysis.print.build.health=true > <W>/gradle.log 2>&1
echo $? > <W>/gradle.rc
```
```bash
rm -f <W>/gradle.rc; touch <W>/verify.start; nohup bash <W>/verify.sh >/dev/null 2>&1 &
for i in $(seq 1 30); do [ -f <W>/gradle.rc ] && break; sleep 10; done; cat <W>/gradle.rc 2>/dev/null || echo RUNNING; tail -n 2 <W>/gradle.log
```
Repeat the poll while it prints `RUNNING`. Then judge:
```bash
grep -oE "Execution failed for task '[^']+'" <W>/gradle.log | sort -u
[ build/reports/dependency-analysis/build-health-report.json -nt <W>/verify.start ] && echo FRESH || echo STALE_REPORT
jq -r -f <W>/kinds.jq build/reports/dependency-analysis/build-health-report.json | cut -f1,2,6 | sort -u
```
`-Pdagp.severity=fail` fails `:buildHealth` on ANY remaining advice, so judge by content. **Pass** when the
only failed task is `:buildHealth` (or none), the report is `FRESH`, and each remaining advice row is either
in the ledger as `human`/`stale`, or pre-existing and not ours — in a module we did not edit, about an
identifier we did not touch (develop moved after the nightly; list it as "pre-existing"). Anything else is
ours: an `applied`/`excluded` row that is still reported, new advice in a module we edited, or new advice
anywhere about an identifier we touched (typically a dependent that used an `api` export we removed).

### 5c. Repair — at most 2 rounds, then revert

- Read the errors (`grep -nE -A4 "^e: |What went wrong" <W>/gradle.log | head -80`), fix our edit (wrong
  accessor, dependent needs the dependency declared, a false positive → revert that line and exclude it),
  re-run 5a. Never retry the same edit unchanged.
- After 2 failed rounds: revert the offending modules (`git checkout <B> -- <file>`, and delete
  the root `exclude` lines added for them), mark them `human: verification failed (<task>: <first error>)`,
  re-run 5a once for what remains. Still failing → `failed`.
- A failure in a module we did not edit that does not involve a touched identifier may be develop's own:
  `git diff > <W>/edits.patch`, `git checkout <B> -- <edited files>`, re-run only the failing task.
  It fails on pristine develop → `failed` with "develop itself fails `<task>`". Otherwise `git apply <W>/edits.patch`
  and treat it as ours. Non-compile tasks (e.g. `:core:ui:verifyDesignTokens` after a submodule pointer
  moved) → `failed`, naming the task.
- Nothing left applied or excluded → `nothing-to-fix`.

## Phase 6: Commit and sanity checks

```bash
git add <each edited build.gradle.kts>                 # never -A / .
git commit -m "Apply dependency analysis advice"      # never --no-verify
[ "$(git log -1 --format=%B)" = "Apply dependency analysis advice" ] && echo MESSAGE_OK || echo BAD_MESSAGE
[ "$(git rev-list --count <B>..HEAD)" = 1 ] && echo BASE_OK || echo BAD_BASE
test -z "$(git diff --name-only <B>..HEAD | grep -vE '(^|/)build\.gradle\.kts$')" && echo ONLY_BUILD_FILES || echo UNEXPECTED_FILE
test -z "$(git diff -U0 <B>..HEAD -- build.gradle.kts | grep -E '^-([^-]|$)|^\+[^+].*(severity|ignoreSourceSet|bundle\(|useTypesafe)')" && echo ROOT_OK || echo ROOT_EDIT_BEYOND_EXCLUSIONS
```
Any token other than `MESSAGE_OK`, `BASE_OK`, `ONLY_BUILD_FILES`, `ROOT_OK` → `failed` (name the token).
Do not push.

## Phase 7: Push and open the PR

Write `<W>/pr-body.md` (Write tool). Line 2 is one line of 1–3 sentences, ≤ 500 characters
(`sed -n 2p <W>/pr-body.md | wc -m`): which modules got which advice, what was excluded and why, what
was left. Too long → name modules, not advice lines; the diff has the rest. Footer only with `--run-url`.
Example:

```
## What
Declared okio in :spend:datasource and removed the unused domain:wallets:models from :data:staking; silenced :data:foo in :features:bar:impl (Hilt bindings only). Left the redundant kapt plugin in :app for a human.

Nightly run: <run-url>
```

```bash
git push -u origin <branch>                            # never --force
gh pr create --base develop --head <branch> --title "Apply dependency analysis advice" --body-file <W>/pr-body.md
```
Capture the printed PR URL. Push or PR failure → `failed` with the reason (the local commit stays). Do
not retry blindly. **Dry-run**: run neither — print both commands verbatim in the summary.

## Phase 8: Return and report

`git checkout -f "$(cat <W>/orig-ref)"` — `-f` is safe: preflight proved the tracked tree clean, so any
leftover change is this run's. Keep the local `tech/dagp_autofix_*` branch when it has the commit; delete
it (`git branch -D`) when it has none.

Final message (example rows):

```markdown
DAGP_AUTOFIX_OUTCOME=pr-opened
DAGP_AUTOFIX_HUMAN=1
### DAGP autofix
**Branch:** `tech/dagp_autofix_2026-09-25` · **PR:** <url>   (dry-run: "local only, not pushed" + the two commands)

| Module | Advice | Result |
|---|---|---|
| `:spend:datasource` | declare `implementation(deps.okio)` | applied |
| `:features:bar:impl` | unused `implementation(projects.data.foo)` | excluded — Hilt bindings only |
| `:app` | redundant plugin `kotlin-kapt` | left for a human — plugin advice |
| `:features:x` | declare `implementation(deps.y)` | already on develop |

**Verification:** `./gradlew buildHealth --continue -Pdagp.severity=fail -Pdependency.analysis.print.build.health=true` → passed in <duration> (pre-existing advice, if any: <modules>)
```
`skipped-open-pr`: the open PR URL. `nothing-to-fix` / `failed`: one `**Reason:**` line, plus the table
when advice was read.

## Never

`./gradlew clean`; touching submodules; editing anything outside the files listed in Constants;
`git add -A`; `--no-verify`; force-push; stashing or discarding changes that predate this run; asking a
question; retrying the same failing step unchanged; printing secrets.