---
name: analyze-logs
description: Analyze Tangem app user logs — extract device info, navigation path, errors, and key events timeline. Use when user provides a log file for bug investigation.
allowed-tools: Read, Grep
argument-hint: /path/to/logfile.txt [/path/to/logs.rtf]
---

Analyze the Tangem app user log file at path: `$ARGUMENTS`

## File Input

The user provides one or two file paths:
- **Log file** (`.txt`) — main application log, always required
- **User info file** (`.rtf` or `.txt`) — optional, contains card/device/error info from the user's feedback email

If two paths are provided, the first is the log file and the second is the user info file.

**If only the log file is provided**, ask the user if they have a user info file (`logs.rtf` or `logs.txt`). If they don't have it or don't respond, fill Device Context, Card Info, and Transaction Context sections from the log file data (Steps 2+3). Mark fields that could not be determined as "N/A".

## User Info File (logs.rtf / logs.txt)

If a user info file is provided, Read it and extract the plain text fields. The file contains structured key-value pairs like:

```
Card ID: AF36000002151580
Firmware version: 6.33r
Linked cards count: 2
Has seed phrase: true
Signed hashes [secp256k1]: 0
----------
Blockchain: Polygon
Explorer link: https://polygonscan.com/address/0x...
Derivation path: m/44'/60'/0'/0/0
Host: https://rpc-mainnet.matic.quiknode.pro/
Token: USDC
Error: Could not construct a recoverable key.
----------
Source address: 0x...
Destination address: 0x...
Amount: 11.319684
Fee: 0.004973
----------
Phone model: SM-S921B
OS version: 36
App version: 5.34.1
```

Extract all fields and include them in the **Device Context**, **Card Info**, and **Analysis Summary** sections of the report. If the RTF contains an `Error:` field, treat it as a key clue for the investigation.

Note: RTF files contain formatting markup (`\cb3`, `\cf4`, `{\field{...}}`). Ignore all RTF tags — only extract the plain text values after each colon.

## Log Format

Each line follows the pattern:
```
DD.MM HH:MM:SS.mmm: TAG Message
```

- Date format: `DD.MM` (day.month), no year — infer from context
- Multi-line entries (JSON bodies, stack traces) continue without the timestamp prefix
- Sensitive data is masked with `******`
- Continuation lines may start with `|` for structured data: `|- Duration millis: 300000`

## Analysis Steps

Use `head_limit` on every Grep call to protect context from overload.

### Step 1: Setup

1. **Log time range:** Read the first and last lines with dates (format `DD.MM`)
2. **Ask the user** (report time range, then ask):
   - Date range to focus on (or "all" for the full file)
   - Focus area: `Wallet`, `WalletConnect`, `Express (Onramp/Buy, Offramp/Sell, Swap/Exchange)`, `TangemPay`, `Feed`, `Markets`, `Settings`, `Referral`, `Staking`, `Onboarding`, `Send/Transactions`, `NFT`, or `all`
   - Remember the chosen area as **FOCUS_AREA**
3. **Determine line range** (skip if "all"):
   - Parse input into `DD.MM` patterns (`10-13.03` → start `10.03`, end `13.03`; `last day` → last date; `last 3 days` → 3 days before last)
   - Find **START_LINE**: Grep `^START_DATE` (head_limit: 1, -n: true)
   - Find **END_LINE**: Grep `^NEXT_DATE` (head_limit: 1, -n: true). If not found, END_LINE = end of file
   - Report: "Focusing on lines START_LINE–END_LINE covering DD.MM–DD.MM"

### Steps 2+3+4+5+6: Main Analysis (all in parallel)

Launch ALL Grep calls below in parallel. Steps 2+3 search the **full file** (device info may be before the date range). Steps 4+5+6 use `offset: START_LINE` to stay within the date range.

**Device Context (full file):**
- `PATCH.*user-wallets/applications` (head_limit: 5, -A 10) — Read JSON body to extract `systemVersion`, `version`, `language`, `timezone`
- `ip_address` (head_limit: 5, -A 20) — extract `alpha2`, `country`, `isBuyAllowed`, `isSellAllowed`

**Card Info (full file):**
- `CardSDK_Tlv.*TAG_Firmware` (head_limit: 20)
- `CardSDK_Tlv.*TAG_SettingsMask` (head_limit: 20)
- `CardSDK_Tlv.*TAG_IsActivated` (head_limit: 20)
- `CardSDK_Tlv.*TAG_ManufacturerName` (head_limit: 20)

**Navigation (offset: START_LINE):**
- `AppRouter` (head_limit: 200) — if FOCUS_AREA is `all` or navigation-heavy (Wallet, Onboarding, Send/Transactions), also Read `.claude/docs/navigation-graph.md` to cross-reference routes

**Errors (offset: START_LINE, head_limit: 50 each, -n: true):**
- HTTP errors: `<-- [45]\d{2}`
- Domain errors: `DomainError`
- App exceptions: `\bException\b`
- Biometric errors: `onAuthenticationError`
- Tangem Pay errors: `Failed checkCustomerWallet`

**Session timeline (offset: START_LINE, head_limit: 50 each):**
- `MainActivity.*onCreate` — app session start
- `MainActivity.*Splash screen` — splash screen installed/dismissed
- `MainActivity.*onNewIntent` — deep link or push notification
- `CardSDK_Session.*start card session` — NFC session starts

**Error filtering:** When processing error results, skip these noisy matches:
- `java.io.IOException: Canceled` — normal request cancellation
- `HttpException(code=304` — HTTP "Not Modified"
- Bare stacktrace lines starting with `\tat`
- `<-- HTTP FAILED: java.io.IOException: Canceled`

### Step 7: Deep Dive

For each significant error found above:
1. Note the error's line number from Grep output (`-n: true`)
2. Use Read with `offset: ERROR_LINE - 100, limit: 200` to get ~200 lines of context
3. In that context, look for navigation events, API calls, and redux actions

## Key Tags Reference

| Tag | Purpose |
|-----|---------|
| `MainActivity` | Activity lifecycle, splash screen, onNewIntent |
| `AppRouter` | Navigation: Push, Pop, Replace |
| `NetworkLogs` | HTTP requests/responses (OkHttp) |
| `BlockchainSDK_NETWORK` | Blockchain RPC calls |
| `CardSDK_Tlv` | NFC card data (firmware, settings) |
| `CardSDK_Session` | NFC session lifecycle |
| `CardSDK_Biometric` | Biometric authentication |

## Common Error Patterns

| Pattern | Meaning |
|---------|---------|
| `HttpException(code=4xx/5xx, errorBody={...})` | API error with structured body |
| `DomainError(description=...)` | App-level domain error |
| `<-- HTTP FAILED: java.io.IOException: Canceled` | Cancelled network request (noise) |
| `<-- 429` | Rate limiting |
| `onAuthenticationError` | Biometric auth failure |

## Output Template

Structure your report EXACTLY as follows:

```
# Log Analysis Report

## Device Context
| Parameter | Value |
|-----------|-------|
| App version | ... |
| Android version | ... |
| Phone model | ... (from logs.rtf if available) |
| Language | ... |
| Timezone | ... |
| Country | ... |
| Log time range | DD.MM HH:MM — DD.MM HH:MM |

## Card Info
| Parameter | Value |
|-----------|-------|
| Card ID | ... (from logs.rtf if available) |
| Firmware | ... |
| Manufacturer | ... |
| Is activated | ... |
| Linked cards | ... (from logs.rtf if available) |
| Has seed phrase | ... (from logs.rtf if available) |

## Transaction Context (from logs.rtf, if available)
| Parameter | Value |
|-----------|-------|
| Blockchain | ... |
| Token | ... |
| Source address | ... |
| Destination address | ... |
| Amount | ... |
| Fee | ... |
| Error | ... |

## Navigation Path
1. [HH:MM:SS] Screen (Push/Pop/Replace)
2. ...

**Summary:** Brief description of the user's journey.

## Errors Found

### HTTP Errors
| Time | URL | Status | Details |
|------|-----|--------|---------|

### Domain Errors
| Time | Component | Error |
|------|-----------|-------|

### Other Errors
| Time | Type | Details |
|------|------|---------|

## Key Events Timeline
| Time | Event | Details |
|------|-------|---------|
(chronological: app starts, card sessions, navigation, errors, notable API calls)

## Analysis Summary
(2-3 paragraphs: what the user was doing, what broke, probable cause, recommendations.
If FOCUS_AREA was specified, emphasize errors, navigation, and API calls related to that area.)
```

If a section has no data, write "None found" instead of omitting it.