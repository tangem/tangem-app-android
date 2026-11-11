---
name: run-e2e-test-locally
description: Run Tangem Android E2E UI tests (app/src/androidTest/.../tests/e2e, classes ending in E2ETest) on a local emulator against a specific network. Use when asked "прогони e2e тесты на сеть {network}", "run e2e tests on {network}", "запусти E2E на {network} локально", "прогони e2e локально", or to reproduce/debug an E2E failure outside CI. Covers fetching that network's catalog (seed/wallet/network/token) from qa-tools through the browser — and, when qa-tools has no data for the named network, asking the user to fill it in and offering the exact fields — then injecting it into E2eCatalog.kt, building + running on the emulator, and reverting the seed afterwards. NOT for the WireMock-backed UI tests (use write-ui-test) or for triaging the nightly CI run.
---

# Run an E2E test locally (Android)

E2E tests (`app/src/androidTest/kotlin/com/tangem/tests/e2e/**`, classes ending in `E2ETest`) run against **production** with WireMock disabled (`setupHooks(useWireMock = false)`) and the send tests **broadcast real on-chain transactions**. The committed `E2eCatalog.kt` ships a non-funded junk placeholder seed, so a local run needs the real catalog, which lives only in qa-tools.

## ⚠️ Before you start

- **Send/token/custom-fee E2E tests broadcast a REAL transaction** from a shared funded wallet. Confirm with the user before running one.
- **The real seed must NEVER be committed.** You inject it into `E2eCatalog.kt` only for the run and revert it immediately after (see step 5).

## 1. Pick the emulator (and don't run on a physical phone)

- Use an **API 35 (`tangem_test`) emulator**. API 37 / `Pixel_9` fails at launch with `NoSuchMethodException android.hardware.input.InputManager.getInstance`.
- `adb devices -l` often shows **both** the emulator and a USB-attached phone. `connectedCheck`/Marathon would target every device, so **always pin the emulator**:
  ```bash
  export ANDROID_SERIAL=emulator-5554   # the emulator's serial from `adb devices`
  ```
- Boot it if needed: `emulator -avd tangem_test -no-snapshot-load &` then `adb -s emulator-5554 wait-for-device`.

## 2. Get the named network's catalog data from the browser (qa-tools)

The user names the network: **"прогони e2e тесты на сеть {network}"**. Turn `{network}` into the catalog slug (trim + lowercase, e.g. `Polygon` → `polygon`, `Arbitrum One` → `arbitrum one`).

qa-tools sits behind **Cloudflare Access**, so `curl` only returns the login page — a browser session on the WARP-enrolled device is required. Use Claude-in-Chrome (the user's authenticated Chrome):

1. `tabs_context_mcp` → `navigate` to `[REDACTED_ENV_URL]<slug>`.
2. `get_page_text` → read the response:
   - **Valid catalog** → JSON with `seedPhrase`, `walletId`, and `network` (`coinId`, `searchQuery`, `tokenName`, `networkName`, `mainScreenToken`, `defaultAddress`, `customAddress`, optional `sendAmount`, and an optional `token` block). Proceed to step 3.
   - **"Sign in · Cloudflare Access" HTML** → the browser isn't authenticated; ask the user to open the URL once in Chrome and log in, then retry.
   - **`{"error":"Unknown network: <slug>", "available":[…]}` (HTTP 400)** → qa-tools has no data for this network. **Do NOT invent values.** Stop and ask the user to fill it in the qa-tools **E2E Tests 🧪** tab (`[REDACTED_ENV_URL]` → E2E Tests → pick/add the network → save), and offer the exact fields (below). Mention which networks already exist from the `available` list.

### Fields to request when the network is missing

The shared seed is set once at the top of the E2E Tests tab and the `walletId` is derived from it. Per network:

- **Network key** — the slug used in `?network=` (e.g. `polygon`).
- **coinId** — Tangem coin id of the native coin (e.g. `polygon-ecosystem-token`).
- **searchQuery** — symbol to search in Manage Tokens (e.g. `POL`).
- **tokenName** — row title in search results (e.g. `POL (ex-MATIC)`).
- **networkName** — network name to enable (e.g. `Polygon`). The Manage-Tokens switch label is this uppercased (`POLYGON`), derived locally.
- **mainScreenToken** — name shown on the main screen (e.g. `Polygon`).
- **defaultAddress** / **customAddress** — the wallet's receive addresses for account 1 and account 2 (sender/recipient for the send tests).
- **sendAmount** (optional; defaults to `0.1`) — native-coin amount the send test transfers.
- **token** (only if the token Send / custom-fee test should run): **token.coinId** (e.g. `usd-coin`), **token.searchQuery** (`USDC`), **token.tokenName** (`USDC`), **token.mainScreenToken** (`USDC`), **token.contractAddress** (the ERC-20 contract on this network), **token.sendAmount** (e.g. `0.01`).

Preconditions for a green run: the sender account must hold the native coin (send + custom-fee tests, plus gas) and, for the token test, the token balance plus native coin for gas. After the user saves, re-fetch `?network=<slug>` and continue.

## 3. Inject the data into E2eCatalog.kt

Write the JSON into `app/src/androidTest/kotlin/com/tangem/common/constants/E2eCatalog.kt`, mirroring exactly what `.github/scripts/update-e2e-catalog.sh` generates: `SEED_PHRASE`, `WALLET_ID`, `E2eNetwork(...)` with `manageTokensNetworkLabel = networkName.uppercase()`, `accountAddresses = listOf(defaultAddress, customAddress)`, and `token = E2eToken(...)` (or `token = null`). Keep the real seed only in the working copy — **do not stage this file**.

## 4. Build + run on the emulator

Only `androidTest` sources change for test edits, but the main mocked APK must be installed too. The one-shot gradle task builds, installs, and runs on the pinned emulator:

```bash
export ANDROID_SERIAL=emulator-5554
./gradlew :app:connectedGoogleMockedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.tangem.tests.e2e.<E2EClassName> \
  --console=plain
```

Run it in the background and wait for the completion notification (first build ~3–8 min). A green run of a send test means a real transaction went out; the explorer link is logged (`E2E-SEND txUrl=…`) and recorded as an Allure step name.

**Marathon-free alternative** (faster re-runs when APKs are already installed): `adb -s emulator-5554 shell pm clear com.tangem.wallet.mocked` then `adb -s emulator-5554 shell am instrument -w -r -e class com.tangem.tests.e2e.<Class> com.tangem.wallet.mocked.test/com.tangem.common.HiltTestRunner`. `am instrument` does **not** clear app data between runs — always `pm clear` first or onboarding is skipped.

## 5. Revert the seed (always)

```bash
git checkout -- app/src/androidTest/kotlin/com/tangem/common/constants/E2eCatalog.kt
```

Verify the real seed is gone (grep for a word from the seed returns nothing) and that `git status` shows no change to `E2eCatalog.kt` before any commit/push.

## 6. Debugging a failure — use the screenshot

When an E2E test fails, **look at the screen, don't reason from the assertion text alone**. Kaspresso + the Allure plugin write a screenshot per step and on failure to the device; the newest is the failure state:

```bash
SP=<scratch>
newest=$(adb -s emulator-5554 shell 'ls -t /sdcard/Documents/allure-results/*-attachment.png | head -1' | tr -d '\r')
adb -s emulator-5554 pull "$newest" "$SP/fail.png"
```

The Allure result JSON on the device (`/sdcard/Documents/allure-results/*-result.json`) maps the step tree and pass/fail per step — pull and parse it to see exactly which step broke (e.g. "Expand 'USDC' and enable the 'POLYGON' network"). For a live frame at any moment: `adb -s emulator-5554 exec-out screencap -p > frame.png`. The HTML report path is printed by gradle: `app/build/reports/androidTests/connected/.../index.html`; the full stack is in `app/build/outputs/androidTest-results/connected/.../*.xml` (`<failure>`), which names the `E2eScenarios.kt:<line>` frame.

The `tangem_test` emulator's internet is flaky and drops mid-session — a "main screen won't load" is usually lost connectivity, not test logic (`adb shell ping -c1 -W2 8.8.8.8`; restore with `adb shell svc data enable; adb shell svc wifi enable`).

### Manage-Tokens gotchas (fixed in [REDACTED_TASK_KEY] — reference when a token/network add flakes)

- **Ambiguous symbol match**: a symbol like `USDC` recurs across dozens of rows (bridged variants, meme tokens). Match the genuine coin by its exact *title* (the row's first `Text`), not `hasText(symbol)`.
- **Row clipped under the sticky search bar**: the first result renders behind the search bar overlay, so `performScrollTo()` is a no-op and the tap misses. `performScrollToIndex(0)` on the results list lifts it clear.
- **Row collapses again on account 2**: after the first account's save, the portfolio fetches balances and the list recomposes, collapsing the expanded token. (Re-)expand and locate the switch in one retry loop (network switches only render under an expanded token — a reliable "expanded" signal).
- **Fee selector tapped too early**: the fee keeps computing after the confirm screen appears; wait for the ⇕ `selectFeeIcon` (renders only once the fee is loaded/selectable) before tapping the selector.

## Running on CI instead

To run E2E on CI (proper environment, catalog auto-regenerated from qa-tools): `gh workflow run run_ui_tests.yml -r <branch> -f <...>` with the E2E class(es) / `E2E_NETWORK`. CI forces a single device and serializes E2E runs — don't use it as the debug loop; iterate locally and use CI only for final verification.