---
name: fix-crashlytics
description: Auto-fix Crashlytics crashes from Jira — find [Crashlytics] tasks, analyze crash, fix code, create branches, comment on Jira. Runs on CI without prompts.
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent, mcp__atlassian__getAccessibleAtlassianResources, mcp__atlassian__searchJiraIssuesUsingJql, mcp__atlassian__getJiraIssue, mcp__atlassian__addCommentToJiraIssue, mcp__firebase__crashlytics_get_issue, mcp__firebase__crashlytics_list_events, mcp__firebase__firebase_get_environment
argument-hint: [--dry-run] [--since <JQL date expression>]
---

Auto-fix Crashlytics crashes reported in Jira.

**CRITICAL: This skill runs on CI. NEVER ask questions. If anything is ambiguous, make the safer choice or skip the task.**

## Constants

- **Jira cloudId**: `tangem.atlassian.net`
- **Firebase project**: `tangemapp`
- **Crashlytics appId (Release)**: `1:721920782444:android:2202a761840271413f2849`
- **Dry-run mode**: check if `$ARGUMENTS` contains `--dry-run`. In dry-run mode, do NOT push branches and do NOT comment on Jira.
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

### 0c. Verify Git State

Run:
```bash
git status --porcelain 2>&1
```
- If output is empty (clean working tree) — OK.
- If there are uncommitted changes — STOP with: `FATAL: Working tree is not clean. Commit or stash changes before running this skill.`

### 0d. Sync with Remote

```bash
git fetch origin
git checkout develop
git pull origin develop
```

Initialize an internal results list to track each ticket's outcome.

## Phase 1: Find Crashlytics Tasks

Search Jira for Crashlytics tasks created in the past day:

- Tool: `mcp__atlassian__searchJiraIssuesUsingJql`
- `cloudId`: `tangem.atlassian.net`
- `jql`: `project = "AND" AND summary ~ "\\[Crashlytics\\]" AND created >= <since value> ORDER BY created DESC`
  - Use the `--since` argument value, or `-1d` if not provided.
- `maxResults`: `50`
- `fields`: `["summary", "status"]`

Collect all returned issue keys (e.g., `[REDACTED_TASK_KEY]`).

If no tasks found, output "No Crashlytics tasks found since <since value>" and stop.

## Phase 2: Filter Out Already-Branched Tasks

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

Keep only tickets that passed both filters.

If no tickets remain after filtering, output the summary table and stop.

## Phase 3: Process Each Ticket

Process each remaining ticket sequentially. **Error handling rule**: if ANY step fails for a ticket, record the failure reason, run `git checkout develop && git checkout -- .` to clean up, and continue to the next ticket.

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
4. `Read` the file. Focus on the method and line number from the blame frame.
5. Use `Grep` to understand related types, method signatures, or null-safety context if needed.

If the file cannot be found: skip with `Skipped (file not found)`.

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

**Rules:**
- Only change the file identified in the blame frame.
- Make the smallest possible change that prevents the crash.
- Use `Edit` tool for precise changes (not `Write` for the whole file).
- Follow existing code patterns in the file (logging, error handling style).
- Do NOT add comments explaining the fix — the commit message and Jira comment handle that.

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

### Step 3f: Create Branch, Commit, Push

```bash
git checkout develop
git checkout -b bugfix/<TICKET_KEY>
git add <changed_files_only>
git commit -m "<TICKET_KEY> Fix <ExceptionType> in <ClassName>"
```

If NOT in `--dry-run` mode:
```bash
git push -u origin bugfix/<TICKET_KEY>
```

Return to develop for the next ticket:
```bash
git checkout develop
```

### Step 3g: Comment on Jira

If NOT in `--dry-run` mode, call `mcp__atlassian__addCommentToJiraIssue`:
- `cloudId`: `tangem.atlassian.net`
- `issueIdOrKey`: the ticket key
- `contentFormat`: `markdown`
- `commentBody`:
  ```
  **Claude Report**
  **Root cause:** <description of what caused the crash>
  **Fix:** <description of the code change>
  **Branch:** bugfix/<TICKET_KEY>
  **Affected file:** <relative path to the changed file>
  ```

Record status as `Fixed`.

## Phase 4: Output Summary

Output the results as a Markdown table:

```markdown
## Crashlytics Auto-Fix Summary

| Ticket | Crash | File | Status | Branch |
|--------|-------|------|--------|--------|
| AND-XXXXX | NPE in ClassName.method | ClassName.kt | Fixed | bugfix/AND-XXXXX |
| AND-YYYYY | IOOB in OtherClass.method | OtherClass.kt | Skipped (branch exists) | — |
| AND-ZZZZZ | ISE in ThirdClass.method | ThirdClass.kt | Failed (build failed) | — |
```

After the table, output totals:
```
**Total:** X tasks found, Y fixed, Z commented (SDK), W skipped, V failed
```