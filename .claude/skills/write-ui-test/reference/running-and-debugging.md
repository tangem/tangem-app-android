# Building, running, and debugging instrumentation tests

## Both APKs matter

Instrumentation tests need TWO APKs:

- `:app:assembleGoogleMocked` → `app-google-mocked.apk` — production code under test
- `:app:assembleGoogleMockedAndroidTest` → `app-google-mocked-androidTest.apk` — the test code

If you change production code and rebuild only the test APK, **the installed main APK stays old** and
your fix doesn't take effect. Symptom: "the fix doesn't help" — except it does, you just ran the
unfixed build.

```bash
# Build both
./gradlew :app:assembleGoogleMocked :app:assembleGoogleMockedAndroidTest
# Install each
adb install -r -t <path-to-app-google-mocked.apk>
adb install -r -t <path-to-app-google-mocked-androidTest.apk>
```

## Run a single test (manual)

```bash
adb shell pm clear com.tangem.wallet.mocked
curl -X POST http://localhost:8081/__admin/scenarios/reset
adb shell am instrument -w \
  -e class "com.tangem.tests.tangempay.TangemPayTest#freezeUnfreezeCard_TogglesCardState" \
  com.tangem.wallet.mocked.test/com.tangem.common.HiltTestRunner
```

## Classify the result — Allure noise vs. real failure

After `pm clear`, `/data/user/0/<pkg>/files/original_screenshots` doesn't exist →
`AllureResultsHack.testRunFinished` throws `NoSuchFileException` → reported as
`Tests run: 1, Failures: 1` with a stack trace starting at `AllureResultsHack`. **This is a post-run
hook failure, NOT a test logic failure.**

Distinguish:
- First stack frame is `AllureResultsHack.testRunFinished` → infra hook noise; ignore it.
- Kaspresso step logs show all `SUCCEED` for steps 1..N → the test passed.
- A REAL failure shows `java.lang.AssertionError` inside the test's own classes
  (e.g. `at com.tangem.tests.X.foo$lambda…`). When auto-classifying CLI output, key off the presence
  of `java.lang.AssertionError` vs. only `original_screenshots`.

## `@Ignore` on instrumentation tests

- Pattern: `@Ignore("https://tangem.atlassian.net/browse/AND-XXXXX")` above `@Test`.
- When ignored, `am instrument -e class …` reports `OK (0 tests)` with `Tests run: 0`
  (NOT `Skipped: 1`). Auto-detection should match the zero-test count.

## WireMock cheatsheet

Local override is detected; otherwise hits remote. Default local port: `8081`.

```bash
# Set a scenario state — PUT, not POST
curl -X PUT http://localhost:8081/__admin/scenarios/<name>/state \
     -H "Content-Type: application/json" -d '{"state":"<state>"}'

# Reset all scenarios
curl -X POST http://localhost:8081/__admin/scenarios/reset

# Inspect
curl http://localhost:8081/__admin/mappings | jq
curl http://localhost:8081/__admin/scenarios | jq '.scenarios[] | {name, state}'
```

- Mocks repo: default to the sibling directory `../tangem-api-mocks/` (i.e. next to
  `tangem-app-android`). If that path doesn't exist, **ask the user** where the mocks repo is rather
  than guessing.
- The repo is **branch-per-suite** — dozens of feature branches (e.g. `send-via-swap-p1`,
  `account-creation`, `swap-express-mocks`, `android-tangem-pay-mocks`). There is no universal
  default branch; check out the one the suite under test expects. If it's unclear which branch holds
  the mappings for your flow, ask the user. Mappings live under `mocks/mappings/`, response bodies
  under `mocks/__files/`.
- **State transitions are atomic per `requiredScenarioState`.** If a scenario defines an `AfterDeposit`
  mapping for `/customer/balance` but not `/customer/me`, a request to `/customer/me` after switching
  to `AfterDeposit` falls through. Check *both* endpoints when an "after" assertion fails.

## Misc

- `./gradlew unitTest` aggregates all debug/googleDebug + JVM-module tests — faster than per-module
  tasks for verifying a broad change (but it's for *unit* tests, not instrumentation).
- Detekt config lives in the `tangem-android-tools` git submodule — look there before assuming a local
  `.detekt.yml`.
- Path discipline: stay in `/Users/maxibello/dev/tangem-app-android`; `cd` into the mocks repo only when
  needed and prefer absolute paths (the shell session resets cwd).