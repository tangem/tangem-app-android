# Running the App on a Device (ADB)

Practical notes for launching the app on a connected device for manual/on-device testing.

## Package & main activity

| | Value |
|---|---|
| Debug package (Google flavor) | `com.tangem.wallet.debug` |
| Main launcher activity | `com.tangem.tap.MainActivity` |
| Application class | `com.tangem.tap.TangemHiltApplication` |

## Launch the main screen (correct way)

```bash
adb shell am force-stop com.tangem.wallet.debug
adb shell am start -n com.tangem.wallet.debug/com.tangem.tap.MainActivity
```

Verify the real screen is in the foreground:

```bash
adb shell dumpsys activity activities | grep -i topResumedActivity | grep -i tangem
# expect: topResumedActivity=ActivityRecord{... com.tangem.wallet.debug/com.tangem.tap.MainActivity ...}
```

## Pitfall: do NOT launch via `monkey`

```bash
# ❌ ambiguous — may open the wrong screen
adb shell monkey -p com.tangem.wallet.debug -c android.intent.category.LAUNCHER 1
```

Debug builds bundle **LeakCanary**, which registers its own `LAUNCHER` activity ("Leaks").
The package therefore has **multiple** MAIN/LAUNCHER activities, so `monkey` (and
`cmd package resolve-activity`) resolves to Android's `ResolverActivity` / the wrong entry
and can open **LeakCanary instead of the app**. Always launch `MainActivity` explicitly with
`am start -n`.

List the launcher activities to confirm:

```bash
adb shell cmd package query-activities \
  -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  | grep -iE "name=" | grep -i tangem
```

## Cold start vs warm start (matters for `Application.init()`)

`TangemApplication.init()` (Hilt setup, and one-time bootstrap such as backend-auth device
key generation + registration) runs **once per process**, on `Application.onCreate()`.

- **Warm start** (process already alive / resumed from background): `init()` does **not** re-run.
  You will NOT see the startup log banner `APP STARTED` or any one-time bootstrap logs.
- **Cold start** (after `am force-stop`, or first launch): `init()` runs → look for the
  `APP STARTED` banner in logcat (tag `TangemApplication`).

To reliably observe anything that happens in `init()`, always `force-stop` first, then launch.

> Note: `init()` runs on process start regardless of *which* activity brought the process up
> (even LeakCanary's). So process-scoped logs are still valid on a wrong-activity launch — but
> for observing actual app UI/flows, launch `MainActivity` explicitly.

## Build & install (Google debug)

```bash
./gradlew :app:assembleGoogleDebug
adb install -r app/build/outputs/apk/google/debug/app-google-debug.apk
```

## Product flavors

Flavors: `google`, `huawei` (dimension `services`); default dev flavor is `google`.
Build types: `debug`, `mocked`, `internal`, `external`, `release`.