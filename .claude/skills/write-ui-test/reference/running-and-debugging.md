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

## Harness: orchestrator vs. raw `am instrument`

The app is configured `execution = "ANDROIDX_TEST_ORCHESTRATOR"` (`app/build.gradle.kts`). The orchestrator
runs **each test method in its own process** (and can clear app data between them). It is still 100%
local — it runs on the same emulator; nothing remote about it.

Raw `adb shell am instrument` runs **all selected tests in one shared process**, which has two failure
modes that look like test bugs but aren't:

- Running several tests in one invocation → `IllegalStateException: There are multiple DataStores active
  for the same file` mid-run. Run them one at a time (with `pm clear` between) if you must use raw
  `am instrument`.
- Tests that re-scan the card inside **Card/Device Settings** (the "Scan card or ring" gate) →
  `IllegalStateException: Tangem SDK is null after re-registering with foreground activity`. The existing
  `ResetCardTest` crashes identically under raw `am instrument`. These only pass via the orchestrator.

**Prefer the orchestrator** (it's what CI/Marathon use). Run a class or method through Gradle:

```bash
./gradlew :app:connectedGoogleMockedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.tangem.tests.DetailsTest
# or a single method: ...class=com.tangem.tests.DetailsTest#someTest
# or several classes:  ...class=com.tangem.tests.DetailsTest,com.tangem.tests.SecurityModeTest
```

Gradle installs both APKs, runs via the orchestrator, then **uninstalls them** — so a following raw
`am instrument` reports `Unable to find instrumentation info`; reinstall both APKs first. Read results
from the JUnit XML (authoritative pass/fail counts), not just stdout:

```bash
ls -t app/build/outputs/androidTest-results/connected/mocked/flavors/google/*.xml | head -1
# inspect tests="…" failures="…" errors="…" skipped="…" and the <testcase>/<failure> nodes
```

## Running against local WireMock

Every instrumentation test runs with `ApiEnvironment.MOCK` (forced in `BaseTestCase.setupHooks`), so the
app's API base URLs point at `wiremock.tests-d.com` — i.e. tests **always** talk to WireMock, never the
real backend. By default that's the **remote** WireMock at `wiremock.tests-d.com`. To use a **local**
WireMock instead, pass `wiremockBaseUrl`: `WireMockRedirectInterceptor` then rewrites every
`wiremock.tests-d.com` request to your local instance.

Emulator addressing matters — `localhost` inside an emulator is the **emulator itself**, not your host:

- Use the host alias **`http://10.0.2.2:8081`** (no extra setup), **or**
- `http://localhost:8081` **with** `adb reverse tcp:8081 tcp:8081` run first.

Pass it through the orchestrator (recommended):

```bash
curl -s -X POST http://localhost:8081/__admin/scenarios/reset   # start clean
./gradlew :app:connectedGoogleMockedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.tangem.tests.DetailsTest \
  -Pandroid.testInstrumentationRunnerArguments.wiremockBaseUrl=http://10.0.2.2:8081
```

(Raw `am instrument` equivalent: `-e wiremockBaseUrl http://10.0.2.2:8081` — subject to the harness
caveats above.)

**If a screen hangs / you get `ComposeNotIdleException` (infinite recomposition):** that usually means a
request the app made wasn't served (endless retry/loading), *not* a test bug. Ask WireMock what it
didn't match — this is the smoking gun:

```bash
curl -s http://localhost:8081/__admin/requests/unmatched | jq '.requests[] | "\(.method) \(.url)"'
```

`unmatched: 0` means the URL plumbing is correct and local WireMock served everything — look elsewhere
(harness/emulator) for the hang. A non-empty list names exactly which mapping (or scenario state) the
local instance is missing.

## When the UI fails silently, the cause is usually app-side — two places to look

A screen failing silently with correct locators (fee shows "—", a banner never appears, a button stays
disabled) is usually missing mock *data* or an app-side gate, not a test bug. Two diagnostics find it:

- **The app's own log is in `files/log.txt`, not logcat** — the mocked build routes `TangemLogger` to a
  file, so `adb logcat` shows nothing. Fastest path to a root cause (e.g. it surfaced
  `IllegalStateException: No native currency found` → a native coin missing from the mock):
  ```bash
  adb exec-out run-as <pkg> cat files/log.txt | grep -iE "Error|Exception|<feature>"
  ```
- **The WireMock journal separates "mock missing" from "app never asked"** —
  `/__admin/requests/unmatched` finds missing mappings, but if `unmatched=0` *and* the expected request
  is also absent from the full log (`/__admin/requests`), the app never issued it (a data/state gate) →
  fix the mock data or the app, not the mappings.

## "UiAutomationService already registered" — retry, it's not a failure

Back-to-back `am instrument` runs sometimes fail instantly with `UiAutomationService … already
registered!` — a teardown race between runs, not a test failure. Retry. (The orchestrator avoids it by
spacing runs — another reason to confirm a flaky-looking suite via the orchestrator, not raw
`am instrument`.)

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

Without a `wiremockBaseUrl` arg the app hits the **remote** WireMock (`wiremock.tests-d.com`); pass the
arg to redirect to a local instance (see "Running against local WireMock"). Default local port: `8081`.

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
- Path discipline: stay at the repo root; `cd` into the mocks repo only when needed and prefer absolute
  paths (the shell session resets cwd).