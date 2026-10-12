# Logging

Log through **`TangemLogger`** (`core/utils` — `com.tangem.utils.logging.TangemLogger`), never
`android.util.Log` / `println` directly.

## Prefer `i` and `e` — only these persist to the log file

`TangemLogger` fans every call out to its registered `LogWriter`s, and each writer decides what it
keeps:

| Writer | Keeps | Where it goes |
|---|---|---|
| `FileLogWriter` (`app/.../common/log`) | **`Severity.Info` and `Severity.Error` only** | `AppLogsStore` → the on-device log file the user can export / attach to a bug report |
| `LogcatLogWriter` | all severities | Logcat (console), **only in `LOG_ENABLED` builds** (debug/mocked/internal) — gone once the process/session ends |

`FileLogWriter` is always registered; `LogcatLogWriter` only in `LOG_ENABLED` builds. So
**`TangemLogger.i(...)` and `TangemLogger.e(...)` are the only levels that survive in the exported
logs.** `v` / `d` / `w` / `a` reach Logcat only in `LOG_ENABLED` builds (debug/mocked/internal) — in
release/external builds they are dropped entirely — and never persist either way.

**Rule of thumb:**

- Something you'd need to diagnose a problem from a user's exported logs — a failure, a caught
  exception, a corrupted-file recovery, an unexpected-but-handled state — log at **`e`** (error) or
  **`i`** (notable info). A `w` (warning) for such a case is a mistake: it never reaches the file.
- Noisy, local, dev-only tracing (`v` / `d`) stays console-only by design — don't promote it to `i`/`e`
  just to see it, or you'll flood the persisted log.

Example — replacing a corrupted DataStore with defaults is a real error worth keeping in the file:

```kotlin
ReplaceFileCorruptionHandler(
    produceNewData = { corruptionException ->
        TangemLogger.e("Preferences DataStore corrupted, replaced with defaults", corruptionException)
        emptyPreferences()
    },
)
```

`shouldSanitize` (default true) is forwarded to `AppLogsStore.saveLogMessage`; pass `false` only when
you deliberately need the raw, unsanitized content in the file.