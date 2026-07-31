---
name: fix-crashlytics
description: Auto-fix Crashlytics crashes from Jira — find [Crashlytics] tasks, verify the crash is still reachable on develop (skip duplicates and already-fixed ones), analyze crash, fix code, open a pull request, comment on Jira. Runs on CI without prompts.
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent, mcp__atlassian__getAccessibleAtlassianResources, mcp__atlassian__searchJiraIssuesUsingJql, mcp__atlassian__getJiraIssue, mcp__atlassian__addCommentToJiraIssue, mcp__firebase__crashlytics_get_issue, mcp__firebase__crashlytics_list_events, mcp__firebase__crashlytics_get_report, mcp__firebase__firebase_get_environment
argument-hint: [--dry-run] [--since <JQL date expression>]
---

Auto-fix Crashlytics crashes reported in Jira.

**CRITICAL: This skill runs on CI. NEVER ask questions. If anything is ambiguous, make the safer choice or skip the task.**

**OUTPUT DISCIPLINE (terse):** Do not narrate steps, restate the situation, or explain reasoning. No preamble, no "I attempted…", no multi-paragraph recaps. On a preflight failure, print ONLY the single `FATAL:` line and stop — nothing else. During processing stay silent; the ONLY human-facing output is the Phase 4 summary table.

## Constants

- **Jira cloudId**: `tangem.atlassian.net`
- **Firebase project**: `tangemapp`
- **Crashlytics appId (Release)**: `1:721920782444:android:2202a761840271413f2849`
- **Dry-run mode**: check if `$ARGUMENTS` contains `--dry-run`. In dry-run mode, do NOT push branches, do NOT open pull requests, and do NOT comment on Jira.
- **PR base branch**: `develop` (all Crashlytics fixes target `develop`).
- **PR labels**: always `bug` (bugfix branch) plus the complexity label `complex` (auto-fixes are single-file, minimal, defensive changes — well within the `complex` file limit).
- **Since**: check if `$ARGUMENTS` contains `--since <value>`. The value is any valid JQL date expression (e.g., `-3d`, `-1w`, `"2026-03-25"`). Default: `-1d`.
- **Tangem SDK packages** (crashes here cannot be fixed in app code):
  - `com.tangem.blockchain` — Blockchain SDK
  - `com.tangem.sdk` — Card SDK
  - `com.tangem.hot.sdk` — Hot SDK
  - `com.tangem.vico` — Vico charting
  - `com.tangem.common.card` — Card SDK common
  - `com.tangem.common.core` — Card SDK common core

## Phase 0: Preflight Checks

Before any work, verify that all required MCP servers and tools are available. **If any check fails, STOP immediately with an error message — do not proceed.**

### 0a. Verify Atlassian MCP

Call `mcp__atlassian__getAccessibleAtlassianResources` (no parameters).
- If the call succeeds and returns a list containing `tangem.atlassian.net` — Atlassian MCP is OK.
- If the call fails or the tool is not found — STOP with: `FATAL: Atlassian MCP server is not connected. Run 'claude mcp list' to check server status.`

### 0b. Verify Firebase MCP

Call `mcp__firebase__firebase_get_environment` (no parameters).
- If the call succeeds and the response contains `project_id: "tangemapp"` — Firebase MCP is OK and connected to the correct project (configured via `.firebaserc`).
- If the project is different or missing — STOP with: `FATAL: Firebase project mismatch. Expected 'tangemapp'. Check .firebaserc configuration.`
- If the call fails or the tool is not found — STOP with: `FATAL: Firebase MCP server is not connected. Run 'claude mcp list' to check server status.`

### 0c. Verify GitHub CLI

Run:
```bash
gh auth status 2>&1
```
- If `gh` is authenticated — OK.
- If the call fails or `gh` is not authenticated — STOP with: `FATAL: gh is not authenticated. Run 'gh auth login'.`

(Skip this check in `--dry-run` mode, since no PR will be opened — but a warning that PRs would be skipped is acceptable.)

### 0d. Verify Git State

Run:
```bash
git status --porcelain 2>&1
```
- If output is empty (clean working tree) — OK.
- If there are uncommitted changes — STOP with: `FATAL: Working tree is not clean. Commit or stash changes before running this skill.`

### 0e. Sync with Remote

Force the local `develop` to **exactly** match the remote. A plain `git checkout develop` + `git pull`
trusts whatever the local `develop` ref already points at — and on the reused self-hosted CI workspace
that ref can be left pointing at another branch (e.g. `master`). If that happens, every fix branch cut
below inherits the wrong base and its PR shows dozens of unrelated commits. `checkout -B … origin/develop`
makes the remote authoritative:

```bash
git fetch origin
git checkout -B develop origin/develop
```

Initialize an internal results list to track each ticket's outcome.

## Phase 1: Find Crashlytics Tasks

Search Jira for Crashlytics tasks created in the past day:

- Tool: `mcp__atlassian__searchJiraIssuesUsingJql`
- `cloudId`: `tangem.atlassian.net`
- `jql`: `project = "CRASHAND" AND summary ~ "\\[Crashlytics\\]" AND created >= <since value> ORDER BY created DESC`
  - Crashlytics auto-tickets live in the dedicated **CRASHAND** project (Firebase Alerts -> Jira). The `summary ~ "[Crashlytics]"` filter excludes test alerts (e.g. `[Firebase] [Test Alert]`).
  - Use the `--since` argument value, or `-1d` if not provided.
- `maxResults`: `50`
- `fields`: `["summary", "status"]`

Collect all returned issue keys (e.g., `CRASHAND-19`).

If no tasks found, output "No Crashlytics tasks found since <since value>" and stop.

## Phase 2: Filter Out Tickets That Need No Work

### 2a. Filter by Existing Branch

For each ticket key, check if a branch already exists:

```bash
git branch -a | grep -F "<TICKET_KEY>"
```

- If a branch is found: record status `Skipped (branch exists)` and remove from the processing list.

### 2b. Filter by Existing Comment

For each remaining ticket, check if it was already processed by a previous run:

- Call `mcp__atlassian__getJiraIssue` with `issueIdOrKey` set to the ticket key and request comments.
- Check if any comment body starts with `**Claude Report**`.
- If such a comment exists: record status `Skipped (already commented)` and remove from the processing list.

### 2c. Filter Out Symbol Duplicates

Crashlytics opens a **new** ticket for every alert, so the same defect comes back as a fresh key
whenever an event arrives from an old build. Before spending work on a ticket, check whether that exact
crash site was already handled.

The ticket summary carries the crashing symbol: `[Crashlytics] [New Fatal Issue] <fully.qualified.Class.method>`.
Keep the **full** symbol — `Class.method` alone collides across packages (several modules define a
`Content`, a `Factory`, a `State`), and a collision here silently skips a live defect.

1. Candidate CRASHAND tickets. Jira text search tokenises on dots, so query by the short form and then
   **confirm on the full symbol**:
   - Tool: `mcp__atlassian__searchJiraIssuesUsingJql`
   - `jql`: `project = "CRASHAND" AND summary ~ "<Class.method>" AND key != <TICKET_KEY> ORDER BY created ASC`
   - `fields`: `["summary", "status", "resolution"]`
   - Discard every hit whose summary does not contain the **exact** `<fully.qualified.Class.method>`.

2. For each surviving candidate `<EARLIER_KEY>`, check whether its fix actually landed on `develop`:
   ```bash
   git log --oneline origin/develop --grep "<EARLIER_KEY>"
   ```
   A non-empty result is the proof — the earlier ticket's commit is on `develop`.

Treat this ticket as a duplicate only when both hold: an earlier ticket carries the exact same symbol
**and** the command above found its commit on `origin/develop`. Then record
`Skipped (duplicate of <EARLIER_KEY>)`, comment (unless `--dry-run`), and remove it from the processing
list.

```
**Claude Report**
**Analysis:** Duplicate of <EARLIER_KEY> — the same crash site (`<Class.method>`) was already fixed by <commit sha> ("<commit subject>").
```

Do **not** skip on a symbol match alone — an earlier ticket with no landed fix means the defect is still
open, so keep processing. Step 3c2 below is the authoritative check.

Keep only tickets that passed all three filters.

If no tickets remain after filtering, output the summary table and stop.

## Phase 3: Process Each Ticket

Process each remaining ticket sequentially. **Error handling rule**: if ANY step fails for a ticket, record the failure reason, run `git checkout -f -B develop origin/develop` to reset back to a clean, authoritative base (discarding the failed ticket's working-tree changes), and continue to the next ticket.

### Step 3a: Extract Crashlytics Issue ID

- Call `mcp__atlassian__getJiraIssue` with `responseContentFormat: "markdown"` to get the full description.
- Find the Crashlytics URL in the description. It looks like:
  ```
  https://console.firebase.google.com/project/tangemapp/crashlytics/app/android:com.tangem.wallet/issues/<ISSUE_ID>
  ```
- Extract `<ISSUE_ID>` from the URL path (the segment after `/issues/` and before `?`).
- If no Crashlytics link found: skip with `Skipped (no Crashlytics link)`.

### Step 3b: Get Crash Details from Firebase

- Call `mcp__firebase__crashlytics_get_issue` with:
  - `appId`: `1:721920782444:android:2202a761840271413f2849`
  - `issueId`: the extracted issue ID
- Call `mcp__firebase__crashlytics_list_events` with:
  - `appId`: `1:721920782444:android:2202a761840271413f2849`
  - `filter`: `{"issueId": "<ISSUE_ID>"}`
  - `pageSize`: `1`

Extract from the response:
- **Exception type and message** (from `subtitle` or `exceptions`)
- **Blame frame**: file name, line number, symbol (method name)
- **Full stacktrace** (from `exceptions` field in events)
- **Crashing build**: `version.displayName` (e.g. `6.0 (1789)`) and the git revision the build was made
  from — `buildStamp.repositories.revision`. **Step 3c2 needs the revision**; if the event carries none,
  that step cannot run and says so instead of guessing (there is no way to map a version name back to a
  commit from the report alone).
- **Crashed thread**: the `threads` entry marked `(crashed)`. A race diagnosis requires evidence of a
  second thread; a single main-thread stack is not one.

> **Line numbers in the blame frame are unreliable.** R8 mangles them — real reports carry values like
> `StateBuilder.kt:2` for a method a hundred lines down. Locate the code by the **symbol** (method name)
> from the blame frame and treat the line number as a hint only.

Then pull the crash's reach, which decides how much this ticket is worth:

- Call `mcp__firebase__crashlytics_get_report` with `report: "topVersions"`, `filter: {"issueId": "<ISSUE_ID>",
  "intervalStartTime": "<90 days ago, ISO 8601>", "intervalEndTime": "<now, ISO 8601>"}`, `pageSize`: `25`.
- Record total `eventsCount` and the versions with non-zero counts. Carry both into the Jira comment and
  the summary table.
- A single event on a single old build is **not** a reason to skip on its own, but it must be reported —
  it is what tells a human this is a latent defect rather than a live regression.

Classify the crash by examining the blame frame and full stacktrace:

1. **App code**: blame frame is in `com.tangem.wallet` with `owner: DEVELOPER`, OR the first `com.tangem` frame in stacktrace is in app packages (`com.tangem.feature.*`, `com.tangem.core.*`, `com.tangem.data.*`, `com.tangem.domain.*`, `com.tangem.tap.*`, `com.tangem.datasource.*`). → Continue to Step 3c (fix the bug).

2. **Tangem SDK**: the first `com.tangem` frame in stacktrace belongs to a Tangem SDK package (see Constants). → Go to Step 3b-sdk (comment only, no fix).

3. **External dependency**: no `com.tangem` frames, or only third-party/Android framework code. → Go to Step 3b-ext (comment only, no fix).

### Step 3b-ext: Handle External Dependency Crash (comment only)

When the crash is in an external dependency (third-party library or Android framework), do NOT attempt to fix it. Instead, comment.

1. Identify the library/framework from the top frames of the stacktrace.

2. If NOT in `--dry-run` mode, call `mcp__atlassian__addCommentToJiraIssue`:
   - `cloudId`: `tangem.atlassian.net`
   - `issueIdOrKey`: the ticket key
   - `contentFormat`: `markdown`
   - `commentBody`:
     ```
     **Claude Report**
     **Crash location:** <library/framework name> — <fully.qualified.class.method>
     **Exception:** <ExceptionType>: <message>
     **Analysis:** This crash originates in an external dependency (<library/framework name>), not in app code.
     ```

3. Record status as `Commented (external dependency)`. Do NOT create a branch.

4. Continue to the next ticket.

### Step 3b-sdk: Handle Tangem SDK Crash (comment only)

When the crash is in a Tangem SDK package, do NOT attempt to fix it. Instead, analyze and comment.

1. Identify which SDK is affected from the package name:
   - `com.tangem.blockchain` → Blockchain SDK
   - `com.tangem.sdk` / `com.tangem.common.card` / `com.tangem.common.core` → Card SDK
   - `com.tangem.hot.sdk` → Hot SDK
   - `com.tangem.vico` → Vico

2. Walk the stacktrace to find the first app-code frame (caller context).

3. Analyze the crash: what exception, what method, what likely input caused it.

4. If NOT in `--dry-run` mode, call `mcp__atlassian__addCommentToJiraIssue`:
   - `cloudId`: `tangem.atlassian.net`
   - `issueIdOrKey`: the ticket key
   - `contentFormat`: `markdown`
   - `commentBody`:
     ```
     **Claude Report**
     **Crash location:** <SDK name> — <fully.qualified.class.method>
     **Exception:** <ExceptionType>: <message>
     **App context:** Called from <app_class.method> at <file:line>
     **Analysis:** <what went wrong — likely cause based on stacktrace and exception message>
     **Recommendation:** This crash originates in Tangem <SDK name>. A fix requires an SDK update.
     ```

5. Record status as `Commented (SDK — <SDK name>)`. Do NOT create a branch.

6. Continue to the next ticket.

### Step 3c: Find and Read the Crashing File

1. Extract the simple class name from the blame frame's `symbol` (e.g., `com.tangem.feature.foo.BarClass.method` -> `BarClass`).
2. Use `Glob("**/<ClassName>.kt")` to find the file.
3. If multiple files match, use the full package path from the stacktrace to disambiguate.
4. `Read` the file. Navigate by the **method name** from the blame frame — the line number is an R8-mangled
   hint, not an address.
5. Use `Grep` to understand related types, method signatures, or null-safety context if needed.

If the file cannot be found: skip with `Skipped (file not found)`.

### Step 3c2: Verify the Crash Is Still Reachable on develop

**Do this before writing any fix.** A crash event is a report about the build it came from, not about
`develop`. Events routinely arrive from builds that are weeks old, so the defect may already be fixed —
in which case the correct output is a comment, not a pull request.

Analysing the crash against `HEAD` while the event came from an older build also produces *invented* root
causes: a guard added after that build gets read as if it had been there, and the write-up ends up
describing a race between a check that did not exist. Read the code **as it was in the crashing build**.

1. Make the crashing revision available (CI clones are shallow):
   ```bash
   git cat-file -e <REVISION>^{commit} 2>/dev/null || git fetch --depth=1 origin <REVISION> 2>/dev/null
   ```
   If the revision cannot be fetched, note `revision unavailable` and continue with the fix — but say so
   in the Jira comment instead of asserting a root cause.

2. Diff the whole file between the crashing revision and `develop` — never a grepped window, which hides
   changes that fall outside it or past the end of a long method:
   ```bash
   git diff <REVISION>..origin/develop -- <FILE>
   ```

3. If the diff is empty, the file is untouched: the crash is reachable. Continue to Step 3d and base the
   root cause on this code.

4. If the diff is non-empty, decide whether it touched the crash site. Read the hunk headers — `git diff`
   labels each with its enclosing declaration (`@@ … @@ fun methodName(`) — and check whether the crashing
   method appears among them. If the hunks are large or the labels ambiguous, extract both versions of the
   method and compare them directly:
   ```bash
   git show <REVISION>:<FILE> > "${TMPDIR:-/tmp}/crash_old.kt"   # then Read both and compare the methods
   ```
   Then find what changed it:
   ```bash
   git log --oneline <REVISION>..origin/develop -- <FILE>
   ```
   - A commit that added a guard, null-check or bounds-check at the crash site → the defect is **already
     fixed**. Record `Skipped (already fixed in <sha>)`, comment (unless `--dry-run`), and move on.
   - Unrelated changes → continue to Step 3d, but write the root cause against the **old** code and note
     in the comment which build the event came from.

Comment body for the already-fixed case:

```
**Claude Report**
**Crash location:** <fully.qualified.Class.method>
**Exception:** <ExceptionType>: <message>
**Event:** <N> event(s), <versions>; crashing build <version> (<revision>)
**Analysis:** The event came from build <version>, built from <revision>. The crash site was fixed on develop by <sha> ("<commit subject>"), which landed after that build was cut — the crashing build did not contain the fix.
**Recommendation:** No code change needed. Close as fixed / duplicate.
```

Never claim a race, a lifecycle conflict or any other mechanism unless the evidence supports it: a race
needs a second thread in the report, and a check-then-use race needs that check to exist in the crashing
build. If the mechanism is unclear, describe what the state was (e.g. "index -1 over an empty list") and
stop there.

### Step 3d: Fix the Bug

Apply a **minimal, defensive fix** based on the crash type. Do NOT refactor, add features, or clean up surrounding code.

**Fix patterns by exception type:**

| Exception | Fix Strategy |
|-----------|-------------|
| `NullPointerException` | Add null-checks. Use `?.` safe calls, `?: return`/`?: default` for fallback. For Moshi-deserialized models where Kotlin non-null types can be JVM-null, cast to nullable: `val x = obj.field as Type?` then null-check. Use `getOrNull()` instead of `[]` for collections. |
| `IndexOutOfBoundsException` | Add bounds checking. Use `getOrNull()`, `firstOrNull()`, `lastOrNull()`. Check `isEmpty()` before indexing. |
| `IllegalStateException` | Check state before access. For `lateinit` crashes: add `::property.isInitialized` check or make property nullable. For Decompose/lifecycle: guard with lifecycle state check. |
| `IllegalArgumentException` | Validate inputs. Use `coerceIn()`, `coerceAtLeast(0)`, `maxOf(0, value)`. For `BigDecimal` formatting issues: handle negative or zero values. |
| `ClassCastException` | Use `as?` safe cast with fallback. |
| `ConcurrentModificationException` | Copy collection before iteration: `.toList()`. |

**Fix every occurrence of the same pattern in that file — not just the line from the stacktrace.**

Fixing only the reported line is what produces a stream of near-identical tickets: the next user hits the
sibling method, Crashlytics files a new alert, and the whole cycle repeats. A real case: the reported
`getSelectedWalletUM()` was guarded while `getSelectedWallet()` and `getSelectedWalletId()` — directly
below it, in the same file, indexing the same list the same unchecked way — were left untouched.

After fixing the blame-frame line, grep the file for the same unsafe construct and fix all of them:

| Exception | What to grep for in the file |
|-----------|------------------------------|
| `IndexOutOfBoundsException` | `[` indexing on lists/arrays, `.first()`, `.last()`, `.single()` |
| `NullPointerException` | `!!`, non-null returns derived from nullable state, `lateinit` reads |
| `ClassCastException` | `as ` hard casts |
| `IllegalStateException` | `error(`, `require(`, `check(`, `lateinit` reads |

Keep the sibling fixes in the same style as the primary one, so the change reads as one decision.

**Rules:**
- Only change the file identified in the blame frame — sibling occurrences **inside that file** are in
  scope, occurrences in other files are not.
- Make the smallest change per occurrence that prevents the crash. No refactors, no renames, no cleanup.
- Use `Edit` tool for precise changes (not `Write` for the whole file).
- Follow existing code patterns in the file (logging, error handling style).
- Do NOT add comments explaining the fix — the commit message and Jira comment handle that.
- If the same pattern also appears in **other** files, do not touch them. Collect the list
  (`file:line`, max 10) and report it in the Jira comment as follow-up work.

```bash
# same pattern elsewhere — report only, never edit
git grep -n "<pattern>" -- '*.kt' | grep -v "<FILE>" | head -10
```

### Step 3e: Build Verification

1. Determine the Gradle module from the file path:
   - Take the path relative to the project root, up to (not including) `src/`.
   - Replace `/` with `:` and prepend `:`.
   - Example: `features/tokendetails/impl/src/...` -> `:features:tokendetails:impl`
   - Special case: `app/src/...` -> `:app` (use `assembleGoogleDebug` instead of `assembleDebug`)

2. Run the build:
   ```bash
   ./gradlew :<module>:assembleDebug
   # or for :app module:
   ./gradlew :app:assembleGoogleDebug
   ```

3. If build fails:
   - Read the error, attempt to fix it (one retry only).
   - If still fails: `git checkout -- .` and skip with `Failed (build failed)`.

### Step 3f: Create Branch, Commit, Push, Open PR

Create the branch **off the authoritative `origin/develop`** — never a local `develop`, which a reused
CI workspace may have left pointing at another branch (that is exactly what makes a PR show dozens of
unrelated commits). `checkout -B … origin/develop` carries the working-tree fix onto the fresh branch:

```bash
git fetch origin develop
git checkout -B bugfix/<TICKET_KEY> origin/develop
git add <changed_files_only>
git commit -m "<TICKET_KEY> Fix <ExceptionType> in <ClassName>"
```

**Sanity check before pushing:** the branch must be exactly **one** commit ahead of `origin/develop`.
If not, the base is wrong — do NOT push; record `Failed (bad base)`, clean up, and skip the ticket:

```bash
git rev-list --count origin/develop..HEAD   # must print exactly 1
```

If in `--dry-run` mode: do NOT push and do NOT open a PR. Instead, print the exact `git push` and
`gh pr create` commands that would run, then return to develop and continue to the next ticket.

If NOT in `--dry-run` mode, push and open the PR:

```bash
git push -u origin bugfix/<TICKET_KEY>
```

Then open the pull request with `gh`. **Base is `develop`**; the **title is the commit subject**
(`<TICKET_KEY> Fix <ExceptionType> in <ClassName>`); the **body must contain NO Jira ticket key** —
not this ticket's key, not any other (the key lives only in the title). Apply both the `bug` and
`complex` labels.

```bash
gh pr create --base develop --head bugfix/<TICKET_KEY> \
  --label "bug" --label "complex" \
  --title "<TICKET_KEY> Fix <ExceptionType> in <ClassName>" \
  --body "$(cat <<'EOF'
## What
Defensive fix for a Crashlytics-reported crash: <one-line description of the crash and the guard added>.

**Root cause:** <short description of what caused the crash, as evidenced by the crashing build's code>
**Fix:** <short description of the code change, including sibling occurrences fixed in the same file>
**Reach:** <N> event(s) on <versions>; crashing build <version>

EOF
)"
```

- **Scrub the body of every Jira key before creating the PR.** Verify with `grep -E "CRASHAND-[0-9]+"`
  (ERE — do not use `\d`); if the body matches, rephrase to remove the key, then create the PR.
- `gh pr create` prints the PR URL on success — **capture it** for Step 3g and the summary.
- If `git push` or `gh pr create` fails: the branch is already committed locally, so record the
  failure reason (`Failed (push failed)` / `Failed (PR failed)`), return to develop, and continue to
  the next ticket. Do NOT retry blindly.

Return to a clean, authoritative develop for the next ticket (pin to the remote again, discarding any
leftover working-tree state so the next ticket's fix is read and applied against the correct base):
```bash
git checkout -f -B develop origin/develop
```

### Step 3g: Comment on Jira

If NOT in `--dry-run` mode, call `mcp__atlassian__addCommentToJiraIssue`:
- `cloudId`: `tangem.atlassian.net`
- `issueIdOrKey`: the ticket key
- `contentFormat`: `markdown`
- `commentBody`:
  ```
  **Claude Report**
  **Event:** <N> event(s), <versions with non-zero counts>; crashing build <version> (<revision>)
  **Root cause:** <what caused the crash, stated against the code as it was in the crashing build>
  **Fix:** <description of the code change, including sibling occurrences fixed in the same file>
  **Branch:** bugfix/<TICKET_KEY>
  **PR:** <PR URL from Step 3f>
  **Affected file:** <relative path to the changed file>
  **Same pattern elsewhere:** <file:line list from Step 3d, or "none">
  ```

Root cause discipline: describe the state that produced the crash and the code path that reached it.
Do not name a mechanism the report does not evidence — no "race", "concurrent", "TOCTOU" unless a second
thread appears in the stacktrace and the interleaving is actually possible in the crashing build's code.

Record status as `Fixed` and store the PR URL for the summary table.

## Phase 4: Output Summary

Output the results as a Markdown table:

```markdown
## Crashlytics Auto-Fix Summary

| Ticket | Crash | File | Events | Status | PR |
|--------|-------|------|--------|--------|----|
| CRASHAND-123 | NPE in ClassName.method | ClassName.kt | 412 on 6.1 (1794) | Fixed | <PR URL> |
| CRASHAND-124 | IOOB in OtherClass.method | OtherClass.kt | 3 on 6.0 (1789) | Skipped (branch exists) | — |
| CRASHAND-125 | ISE in ThirdClass.method | ThirdClass.kt | 1 on 6.0 (1789) | Skipped (already fixed in a1b2c3d) | — |
| CRASHAND-126 | NPE in FourthClass.method | FourthClass.kt | 27 on 6.1 (1791) | Failed (build failed) | — |
```

For the `Events` column: total event count over the last 90 days plus the versions carrying them — this
is what lets a human triage the batch. For the `PR` column: the PR URL for `Fixed` tickets, or `—` for
skipped/commented/failed tickets (and the would-be branch name in `--dry-run`).

After the table, output totals:
```
**Total:** X tasks found, Y fixed (PRs opened), Z commented (SDK/external), W skipped, V failed
```

If any ticket was skipped as already-fixed or as a duplicate, add one line naming them — those are the
tickets a human should close, and they are easy to miss inside the table:
```
**Close without a fix:** CRASHAND-125 (fixed in a1b2c3d), CRASHAND-127 (duplicate of CRASHAND-18)
```