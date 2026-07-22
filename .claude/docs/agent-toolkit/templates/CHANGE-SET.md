# CHANGE-SET block (the apply contract)

> **Why this exists.** In this environment, an `Agent` subagent runs detached. It cannot
> surface an interactive permission prompt, so any `Write` / `Edit` — or a non-allowlisted
> Bash write (`cat >`, `touch`, `>>`) — it attempts is **auto-denied**. Allowlisted Bash
> (e.g. `./gradlew …`) still runs fine. Therefore **subagents never write files.** A
> specialist designs the change and returns it as a CHANGE-SET; the **main loop** (Claude
> Code itself) applies it with `Write`/`Edit`, which is where the user approves each write.
>
> A specialist returns a CHANGE-SET *in addition to* its HANDOFF when its job was to produce
> file changes. Read-only specialists (code-analyzer, verifier, reviewers) return only a
> HANDOFF.

The block must be **deterministically applyable** — the main loop should be able to apply it
mechanically without re-deriving anything. Give exact paths and exact text.

```
## CHANGE-SET — <agent-name> — <YYYY-MM-DD HH:MM>

**Summary:** one line — what this set of changes accomplishes.

### Apply order
1. <file A> (new)
2. <file B> (edit)
3. <bash> register module in settings.gradle.kts
   …list every item below in the order the main loop should apply them…

### New files
For each new file: full path, then the COMPLETE file content in a fenced block.

#### `path/to/NewFile.kt`
```kotlin
<full file content — no elisions, no "// …" placeholders>
```

### Edits to existing files
For each edit: the path, then one or more (old → new) pairs. `old_string` must be an
EXACT, UNIQUE substring of the current file (enough surrounding lines to be unambiguous) so
the main loop can apply it with the `Edit` tool verbatim. No line-number-only references.

#### `path/to/Existing.kt`
- old:
  ```kotlin
  <exact current text, unique in the file>
  ```
  new:
  ```kotlin
  <replacement text>
  ```

### Deletes / renames / bash
Explicit shell commands (the main loop runs them): `git mv …`, `rm …`, etc.

### Post-apply verification (run by the main loop AFTER applying)
The exact commands to confirm the change-set is correct. You could NOT compile it yourself —
the files were not on disk during your run — so list precisely what must be checked:
```bash
./gradlew :features:<name>:impl:compileDebugKotlin
./gradlew :features:<name>:impl:detekt
./gradlew :features:<name>:impl:testDebugUnitTest --tests "<Fqn>"
```

### Risks / assumptions
Anything you could not verify read-only (a symbol you assumed exists, an API shape you
inferred). The main loop checks these first if verification fails.
```

## Rules for producing a good CHANGE-SET

- **No elisions.** New files are complete; edits carry enough context to be unique. A `// …`
  or `/* unchanged */` placeholder makes the set un-applyable — never use one.
- **Edits target the smallest unique anchor**, not whole-file rewrites, so the diff stays
  reviewable and the `Edit` apply is unambiguous.
- **Front-load discovery read-only.** You cannot compile un-applied code, so verify every
  symbol, import, package path, and API shape against the *current* tree with Read/Grep
  before you commit it to the set. Wrong assumptions surface as build failures after apply,
  which costs a full round-trip — minimise them.
- **State what you could not verify** in *Risks / assumptions*. Honesty here is what lets the
  main loop fix a failed apply in one step instead of re-investigating from scratch.