---
name: cleanup-feature-toggles
description: Remove released feature toggles (version <= target) — deletes from config, removes toggle properties, inlines `true` in calling code, removes dead branches. CI-safe, no prompts.
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent
argument-hint: <version> [--dry-run] [--only <TOGGLE_NAME>]
---

Remove all feature toggles whose version is less than or equal to the target release version.

**CRITICAL: This skill runs on CI. NEVER ask questions. If anything is ambiguous, make the safer choice or skip the toggle.**

## Constants

- **Config file**: `core/config-toggles/src/main/assets/configs/feature_toggles_config.json`
- **Generated enum** (DO NOT edit): `core/config-toggles/build/generated/source/toggles/com/tangem/core/configtoggle/FeatureToggles.kt`
- **Dry-run mode**: check if `$ARGUMENTS` contains `--dry-run`. In dry-run mode, make NO file changes — only output what WOULD be removed (including affected files and usage sites).
- **Version**: extract the version number from `$ARGUMENTS` (e.g., `5.35`, `5.35.0`). The version is the first argument that matches a semver-like pattern (`X.Y` or `X.Y.Z`).
- **Only mode**: check if `$ARGUMENTS` contains `--only <TOGGLE_NAME>`. If present, process ONLY the specified toggle (it must still satisfy the version check). Multiple `--only` flags can be provided.

## Phase 0: Preflight Checks

### 0a. Parse Arguments

Extract `<version>`, optional `--dry-run`, and optional `--only <TOGGLE_NAME>` (repeatable) from `$ARGUMENTS`.

- If no version found: STOP with `FATAL: No version provided. Usage: /cleanup-feature-toggles <version> [--dry-run] [--only <TOGGLE_NAME>]`
- Validate version matches pattern `\d+\.\d+(\.\d+)?` — if not, STOP with `FATAL: Invalid version format.`
- Normalize version: if only `X.Y` is given, treat as `X.Y.0` for comparison.
- If `--only` flags are present, collect the toggle names into a filter list.

### 0b. Verify Git State

```bash
git status --porcelain 2>&1
```
- If output is empty (clean working tree) — OK.
- If there are uncommitted changes — STOP with: `FATAL: Working tree is not clean. Commit or stash changes before running this skill.`

Initialize an internal results list to track each toggle's outcome.

## Phase 1: Identify Toggles to Remove

1. Read `core/config-toggles/src/main/assets/configs/feature_toggles_config.json`.
2. For each toggle entry in the JSON array:
   - If `version == "undefined"` → skip (unreleased feature, must not be removed).
   - Parse the toggle's version as semver (normalize `X.Y` to `X.Y.0`).
   - If toggle version **<=** target version → mark for removal.
3. If `--only` filter is active: keep only toggles whose `name` matches one of the `--only` values. If a `--only` toggle doesn't satisfy the version check, output a warning but still skip it.
4. Output the list of toggles marked for removal with their versions.
5. If no toggles match → output `No toggles to remove for version <version>` and stop.
6. If `--dry-run` mode → proceed to Phase 2 (research only, all toggles in parallel), then skip to Phase 7 to output the detailed summary. Do NOT make any file changes.

## Phase 2: Research (parallel)

Collect all information about all toggles **in parallel** before making any edits. Launch one `Agent` per toggle (all in a single message so they run concurrently). Each agent receives the toggle name and must return a structured report.

**Error handling rule**: if research fails for a toggle, record the failure reason and continue. Do NOT stop processing.

### Per-toggle research task (runs inside each Agent)

Each Agent performs the following read-only searches and returns a structured report:

#### 2a. Find the toggle property declaration and direct usages

Use `Grep` to search for `FeatureToggles.<TOGGLE_NAME>` (e.g., `FeatureToggles.WALLET_REORDER_FEATURE_ENABLED`) across the **entire** codebase.

This will find:
1. **`DefaultXxxFeatureToggles` property** — the standard wrapper. Extract:
   - The **property name** (e.g., `isWalletReorderFeatureEnabled`)
   - The **DefaultXxxFeatureToggles file path**
   - The **XxxFeatureToggles interface name** (from the class's supertype)
2. **Direct `FeatureTogglesManager.isFeatureEnabled()` calls** — code that bypasses the wrapper and calls the manager directly. These are additional usage sites.

Also find the interface file:
- Use `Glob` to find the `XxxFeatureToggles.kt` file in `features/*/api/` or `core/*/`

Check if the toggle property or the `DefaultXxxFeatureToggles` class has **comments referencing additional cleanup** (e.g., `// Remove GiveTxPermissionBottomSheet and all dependencies with this toggle`). If found, record the comment text.

If the toggle reference is not found anywhere: report as `Skipped (no property found)`.

#### 2b. Find `@RemoveWithToggle` annotated code

Use `Grep` to search for `RemoveWithToggle` (without `@` or package prefix) across the entire codebase (excluding the annotation definition itself). Then filter matches to only those where the `toggleName` argument equals the current toggle name.

The annotation is defined in `core/utils/src/main/java/com/tangem/utils/annotations/RemoveWithToggle.kt` (`com.tangem.utils.annotations.RemoveWithToggle`). It has two parameters: `toggleName: String` (the toggle name) and `description: String` (optional hint).

Support all Kotlin annotation forms:
- `@RemoveWithToggle("TOGGLE_NAME")`
- `@RemoveWithToggle(toggleName = "TOGGLE_NAME")`
- `@com.tangem.utils.annotations.RemoveWithToggle("TOGGLE_NAME")`
- `@com.tangem.utils.annotations.RemoveWithToggle(toggleName = "TOGGLE_NAME")`

For each filtered match, record:
- The file path and line number
- The annotated element name (class, function, property)
- The `description` value if present

#### 2c. Find all usages of the property in calling code

Use `Grep` to search for the property name (e.g., `isWalletReorderFeatureEnabled`) across the entire codebase.

Categorize results:
- **Interface declaration** — the `val isX: Boolean` in `XxxFeatureToggles.kt`
- **Implementation** — the `override val isX` in `DefaultXxxFeatureToggles.kt`
- **Calling code** — any other file that reads `*.isX` (include file path, line number, and the matched line content)

#### Agent report format

Each Agent must return a report with:
- Toggle name
- Property name (e.g., `isWalletReorderFeatureEnabled`) or `null` if not found
- Interface name and file path
- Implementation file path
- List of calling code sites: `[{file, line, content}]`
- List of direct `FeatureTogglesManager` usage sites: `[{file, line, content}]`
- List of `@RemoveWithToggle` sites: `[{file, line, element, description}]`
- Cleanup comments (if any)
- Status: `ready` or `skipped (reason)`

### After all Agents complete

Collect all reports. If `--dry-run` → skip to Phase 6 with the collected data.

## Phase 3: Edit (sequential)

Process each toggle **sequentially** using the research data from Phase 2. Only toggles with status `ready` are processed.

**Error handling rule**: if ANY step fails for a toggle, record the failure reason and continue to the next toggle. Do NOT stop processing.

### Step 3a: Replace usages in calling code with `true` and simplify

For each calling code usage site (from Phase 2 report), `Read` the surrounding context (at least 20 lines around the usage) and apply the appropriate simplification:

| Pattern | Simplification |
|---------|---------------|
| `if (toggles.isX) { body }` | Remove `if`, keep `body` (unindent) |
| `if (toggles.isX) { A } else { B }` | Keep only `A`, remove if/else structure |
| `if (!toggles.isX) { body }` | Remove entire if-block |
| `if (!toggles.isX) { A } else { B }` | Keep only `B`, remove if/else structure |
| `toggles.isX && expr` | Replace with `expr` |
| `expr && toggles.isX` | Replace with `expr` |
| `toggles.isX \|\| expr` | Replace with `true` (or simplify enclosing condition since it's always true) |
| `val x = toggles.isX` | Replace with `val x = true`, then check if `x` is used in one of the patterns above and simplify transitively |
| `property = toggles.isX` | Replace with `property = true` |
| `when { toggles.isX -> A; else -> B }` | Keep only `A`, remove the `when` structure |
| `when { !toggles.isX -> A; else -> B }` | Keep only `B`, remove the `when` structure |
| `when(value) { ... }` with toggle in a branch condition | Evaluate the toggle to `true`, simplify the `when` accordingly |
| Complex boolean expression | Replace `toggles.isX` with `true` and algebraically simplify |

Also process any direct `FeatureTogglesManager.isFeatureEnabled()` call sites the same way (replace with `true` and simplify).

**After replacing**, check if the file still references the `XxxFeatureToggles` type:
- If not → remove the import of `XxxFeatureToggles`
- If the type was a constructor/inject parameter and is no longer used → remove the parameter and any `@Inject`/`@Assisted` annotations associated with it
- If removing a constructor parameter from a Decompose Model or Component, also remove it from the caller that creates the instance

**Important**: Use `Edit` for precise changes. Read enough context to make correct edits. Do NOT accidentally delete unrelated code.

### Step 3b: Remove the property from interface and implementation

1. **In `XxxFeatureToggles` interface**: remove the `val isPropertyName: Boolean` line.
2. **In `DefaultXxxFeatureToggles`**: remove the `override val isPropertyName: Boolean` property (including the `get() = ...` line).
3. Check if `DefaultXxxFeatureToggles` still has other properties:
   - If **yes** → done with this toggle.
   - If **no** (all properties removed) → the interface and implementation are now empty. Check the **protected list** below — if the interface is protected, keep it and skip deletion. Otherwise, delete them:

**Protected interfaces (never delete even if empty):**
- `TokensFeatureToggles`
- `BlockchainSDKFeatureToggles`
- `StakingFeatureToggles`
- `CardSdkFeatureToggles`
- `TangemPayFeatureToggles`
- `SwapFeatureToggles`
- `SendFeatureToggles`

If the interface is **protected**: remove the `featureTogglesManager` / `featureToggles` constructor parameter from `DefaultXxxFeatureToggles`, remove unused imports (`FeatureTogglesManager`, `FeatureToggles`), but keep both files.

If the interface is **not protected** → delete them:
     1. Delete the `XxxFeatureToggles` interface file.
     2. Delete the `DefaultXxxFeatureToggles` implementation file.
     3. Find and remove the Hilt binding for this interface (typically a `@Binds` method in a `*FeatureTogglesModule` or similar Hilt module). If the Hilt module has no remaining bindings after removal, delete the module file as well.
     4. Use `Grep` to find all remaining references to `XxxFeatureToggles` and `DefaultXxxFeatureToggles` across the codebase. For each reference:
        - **Constructor/inject parameter** → remove the parameter. If the surrounding class/function no longer uses any feature toggles, cascade the removal to its callers.
        - **Import statement** → remove it.
        - **Any other reference** → assess and remove or update as needed.

Record status as `Removed` with the count of usage sites simplified.

## Phase 4: Update JSON Config

1. Read `feature_toggles_config.json`.
2. Remove all entries whose `name` matches a successfully removed toggle (status = `Removed`).
3. Write back the JSON with proper formatting:
   - 2-space indentation
   - Each entry on its own lines
   - No trailing commas
   - Match the existing file format exactly

## Phase 5: Build, Test & Lint Verification

Run all verification tasks in a **single Gradle invocation** to avoid repeated cold starts:

### 5a. Build + Tests + Detekt

```bash
./gradlew assembleGoogleDebug unitTest detekt detektMain :app:assembleGoogleMocked :app:assembleGoogleMockedAndroidTest
```

- If the command **fails**:
  - Read the error output to determine which task failed.
  - **Compilation error** (`assembleGoogleDebug` or `assembleGoogleMocked`): attempt to fix (one retry — usually missing import removal or unused parameter). If still fails: revert all changes with `git checkout -- .` and output `FATAL: Build failed after cleanup. All changes reverted.` with the error details.
  - **Unit test failure**: attempt to fix (one retry). If still fails: output the failures as warnings in the summary but do NOT revert.
  - **Detekt violation**: attempt to fix (one retry — usually unused imports or parameters). If still fails: output the violations as warnings in the summary.
  - **UI test compilation failure**: attempt to fix (one retry). If still fails: output the failures as warnings in the summary.
  - After fixing, re-run the **full command** to verify everything passes together.

## Phase 6: Branch, Commit, Push & PR

Skip this phase entirely in `--dry-run` mode.

### 6a. Create Branch and Commit

```bash
git checkout -b tech/cleanup-toggles-<version>
git add -A
git commit -m "[Tech] Remove feature toggles <= <version>"
```

Replace `<version>` with the target version (e.g., `tech/cleanup-toggles-5.35`).

### 6b. Push

```bash
git push -u origin tech/cleanup-toggles-<version>
```

### 6c. Create Pull Request

Use `gh pr create` targeting `develop`:

```bash
gh pr create --base develop --title "Remove feature toggles <= <version>" --body "$(cat <<'EOF'
## Summary

Automated cleanup of feature toggles that are permanently enabled (version <= <version>).

### Removed toggles

- `TOGGLE_NAME_1` (version)
- `TOGGLE_NAME_2` (version)
- ...

### Manual review required

<List all @RemoveWithToggle-annotated elements found in Phase 2 research (file path, element name, description) and any cleanup comments. If none found, write "None">

## Test plan

- [x] `assembleGoogleDebug` passes
- [x] `unitTest` passes
- [x] `detekt detektMain` passes

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

Output the PR URL.

## Phase 7: Output Summary

Output results as a Markdown table:

```markdown
## Feature Toggle Cleanup Summary

| Toggle | Version | Interface | Usages Simplified | Status |
|--------|---------|-----------|-------------------|--------|
| WALLET_REORDER_FEATURE_ENABLED | 5.34 | WalletFeatureToggles | 3 | Removed |
| EARN_BLOCK_ENABLED | 5.35 | EarnFeatureToggles | 1 | Removed |
| SOME_TOGGLE | 5.33 | SomeFeatureToggles | — | Skipped (no property found) |

**Total:** X toggles processed, Y removed, Z skipped/failed
**Target version:** <version>
```

### Manual Review Hints

If any toggle had a comment referencing additional cleanup (found in Phase 2 research), output a separate section:

```markdown
### Manual Review Required

- **GASLESS_APPROVAL_ENABLED**: `// Remove GiveTxPermissionBottomSheet and all dependencies with this toggle`
- **OTHER_TOGGLE**: `// Also remove legacy FooBar component`
```

### Dry-run mode output

In `--dry-run` mode: prepend `[DRY RUN]` to the header, set all statuses to `Would remove`, and add a detailed section per toggle:

```markdown
### WALLET_REORDER_FEATURE_ENABLED (5.34) — Would remove

**Property:** `WalletFeatureToggles.isWalletReorderFeatureEnabled`
**Files affected:**
- `features/details/impl/.../UserWalletListModel.kt:42` — `walletFeatureToggles.isWalletReorderFeatureEnabled && userWallets.size > 1`
- `features/wallet/impl/.../SomeOtherFile.kt:88` — `if (walletFeatureToggles.isWalletReorderFeatureEnabled)`
```

### Warnings

If unit tests or detekt failed after fix attempts, list the remaining issues:

```markdown
### Warnings

- **Unit test failure:** `:features:wallet:impl:testDebugUnitTest` — WalletModelTest.someTest (may need manual update)
- **Detekt violation:** UnusedPrivateMember in `SomeFile.kt:15`
```