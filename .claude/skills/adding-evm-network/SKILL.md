---
name: adding-evm-network
description: Use when wiring a new EVM-compatible network into the Android app after it was added to blockchain-sdk-kotlin — network id / coin id mapping, icons, MoonPay/Mercuryo mapping, excluded_blockchains rollout, card curve config, accounts, QuickNode credentials. Also use when a freshly added network misbehaves in the app — no icon, no rates or markets, network missing from the token list, wrong account derivation, not visible on old cards.
---

# Adding an EVM network — app side

## Overview

The chain itself lives in **blockchain-sdk-kotlin** — follow its `adding-evm-network` skill
(`.claude/skills/adding-evm-network/SKILL.md` in that repo) first. The app only *registers* the new
`Blockchain` entries: ~10 files, no new classes. iOS covers SDK + app in one `add-chain` skill; on
Android these are two repos and two PRs.

**Most app edit points are exhaustive `when`s — bumping the SDK breaks the build until they're
covered. Four are not** (`fromNetworkId` and `BlockAidChainNameConverter` → `else -> null`,
`CardSdk.isBlockchainUnsupported` → `else -> false`, `excluded_blockchains_config.json`), and a
wrong *value* in an exhaustive branch (coin id, MoonPay code) compiles just as well. A green build proves only that every branch exists.

**Derive the file set mechanically, never from memory:**

```bash
git grep -l "Electroneum" -- '*.kt' '*.json'   # plain EVM reference chain — full occurrence list
git show --stat dd6bd007e6                      # [REDACTED_TASK_KEY] Electroneum, the reference addition
git show --stat 5c8e5256da                      # [REDACTED_TASK_KEY] Onyx: same set + SDK bump
git show --stat b593a7d3f3                      # [REDACTED_TASK_KEY] Public Masterpiece: mainnet only
git show --stat 6e458067a3                      # [REDACTED_TASK_KEY] Robinhood: + QuickNode creds
```

## Step 0 — Inputs

Take them from the SDK PR and the task; do not invent them.

| Input | Used in | Note |
|---|---|---|
| SDK version with the chain | `gradle/tangem_dependencies.toml` → `tangemBlockchainSdk` | Build of the merged SDK PR (`develop-XXXX` / `releases-X.Y-XXXX`) |
| network id, mainnet + testnet | `fromNetworkId` / `toNetworkId` | Same as the SDK `Blockchain.id` and the backend network id (`foo` / `foo/test`). No testnet in the SDK → mainnet entries only |
| coin id | `toCoinId` | Backend/CoinGecko coin id of the **native** coin — ask backend, don't derive from the name |
| icons | `core/ui/src/main/res/drawable` | From Figma (Blockchains page — active, Logo page — greyed out) |
| release version | `excluded_blockchains_config.json` | First app version where the network is visible |
| private RPC credentials | `GeneratedEnvironmentConfigConverter` | Only if the SDK added `quickNodeFooCredentials`; the keys must already be in `tangem-app-config`, see trap #7 |

## Registration checklist

"Enforced" = exhaustive `when`, the build breaks without it.

| # | File | What to add | Enforced |
|---|---|---|---|
| 1 | `gradle/tangem_dependencies.toml` | bump `tangemBlockchainSdk` | — |
| 2 | `libs/blockchain-sdk/.../utils/Blockchain.kt` — `fromNetworkId()` | `"foo" -> Blockchain.Foo`, `"foo/test" -> Blockchain.FooTestnet` | ❌ `else -> null` — the network silently disappears from backend-driven lists |
| 3 | same — `toNetworkId()` | reverse mapping | ✅ |
| 4 | same — `toCoinId()` | `Foo, FooTestnet -> "<coin id>"` | ✅ value unchecked |
| 5 | `libs/blockchain-sdk/.../utils/TransactionExtras.kt` | EVM `-> NONE` group | ✅ |
| 6 | `libs/crypto/.../derivation/AccountNodeRecognizer.kt` — `isAccountsSupported()` | mainnet → `true` group, testnet → `false` group | ✅ |
| 7 | `domain/card/.../configs/Wallet2CardConfig.kt` | `Foo -> EllipticCurve.Secp256k1` + testnet | ✅ |
| 8 | `common/ui/.../extensions/BlockchainIcons.kt` | `IconSet(active = R.drawable.img_foo_22, greyedOut = R.drawable.ic_foo_22)` | ✅ |
| 9 | `core/ui/src/main/res/drawable/` | `img_foo_22.xml` (colored), `ic_foo_22.xml` (single-color) | — |
| 10 | `data/offramp/.../MoonpayBlockchainMapping.kt` | `MoonPaySupportedCurrency(networkCode, currencyCode)` or `null` | ✅ value unchecked |
| 11 | `data/onramp/.../legacy/MercuryoBlockchainMapping.kt` | Mercuryo network code or `null` | ✅ value unchecked |
| 12 | `core/config-toggles/src/main/assets/configs/excluded_blockchains_config.json` | `{ "name": "foo", "version": "<release>" }` | ❌ without it the network ships visible right away |
| 13 | `domain/card/.../common/extensions/CardSdk.kt` — `isBlockchainUnsupported()` | **only by product decision**, see trap #4 | ❌ `else -> false` |
| 14 | `core/datasource-impl/.../converter/GeneratedEnvironmentConfigConverter.kt` | `quickNodeFooCredentials = QuickNodeCredentials(...)` — only if the SDK has the field | — |
| 15 | `libs/blockchain-sdk/.../utils/BlockAidChainNameConverter.kt` | Blockaid chain name — only if Blockaid supports the chain | ❌ `else -> null` — WalletConnect transactions go unscanned |

Not needed for a plain EVM chain (unlike iOS): tokens, NFT, custom tokens and tx history are
resolved inside the SDK from `isEvm()`, and `AccountNodeRecognizer.getAccountNodeIndex` handles any
EVM chain with the `m/44'/60'` path. Feature-specific network lists (joint account, Polymarket,
gasless, Tangem Pay, staking/swap notifications) are opt-in per feature — a new network doesn't go
there unless the task says so.

## Silent traps

**1. `fromNetworkId` forgotten.** Backend responses (tokens list, markets, swap) carry the network id;
without the mapping the network is dropped as unknown. The SDK bump does not break the build.

**2. Wrong coin id.** Rates, charts and markets are keyed by coin id. There is no naming pattern
(`"arbitrum-one"`, `"base-ethereum"`, `"igra-bridged-kaspa"`), so a guess compiles and shows no
price. Check that the backend knows the id in both quotes **and** markets — they are separate
services, and an id can live in one only.

**3. MoonPay / Mercuryo codes guessed.** Fetch https://api.moonpay.com/v3/currencies and map the chain
only if it is in the response; otherwise `null`. Same for Mercuryo — `null` unless the provider lists it.

**4. Old cards (`CardSdk.isBlockchainUnsupported`).** Adding a chain there hides it on cards with
firmware `<= HDWalletAvailable`. Robinhood was added there and removed again in `9cd263217d`
([REDACTED_TASK_KEY]). Default: don't add it; only on an explicit product decision, and then update
`SupportedBlockchainsTest`.

**5. `excluded_blockchains_config.json` semantics.** `name` is the SDK `Blockchain.id`, matched
exactly — a typo resolves to `Blockchain.Unknown` and the network ships **visible**. It is hidden
while the app version is below `version` (`"undefined"` = hidden forever). Don't add a testnet
entry: `ExcludedBlockchains` excludes the mainnet's `getTestnetVersion()` along with it. Toggle per
network in the Tester menu → Excluded blockchains.

**6. Raster inside an icon.** Convert the Figma SVG to a vector drawable. If the SVG contains an
`<image>` element it carries a raster layer, often hidden under the shapes: **don't** commit the icon —
stop and ask the developer to have designers remove the raster from the Figma component and re-export.

**7. QuickNode keys.** `GeneratedEnvironmentConfig` is generated from the keys of the
`tangem-app-config` JSON, so `quiknodeFooApiKey` / `quiknodeFooSubdomain` exist only once they are in
that config — before that the converter doesn't compile. Don't add empty credential keys there
yourself: ask the admins to add them first, then wire the converter.

## Tests

- `libs/blockchain-sdk/src/test/.../utils/FooBlockchainMappingTest.kt` — mirror
  `ElectroneumBlockchainMappingTest`: `fromNetworkId`, `fromId`, `toNetworkId`, `toCoinId` for mainnet
  and testnet (mainnet only: `PublicMasterpieceBlockchainMappingTest`), expected values as string
  literals.
- `common/ui/src/test/.../BlockchainIconsTest.kt` — add the chain to both expected maps.
- `domain/card/src/test/.../Wallet2CardConfigTest.kt` — add both entries.
- `SupportedBlockchainsTest` — only if `CardSdk.kt` was touched.

## Verification — required before claiming done

```bash
./gradlew :libs:blockchain-sdk:testDebugUnitTest --tests "*FooBlockchainMappingTest"
./gradlew :common:ui:testDebugUnitTest --tests "*BlockchainIconsTest"
./gradlew :domain:card:testDebugUnitTest --tests "*Wallet2CardConfigTest"
./gradlew :app:assembleGoogleDebug
./gradlew detektDebug detektMain
```

Then diff the registration set against the reference chain:

```bash
for f in $(git grep -l "Electroneum" -- '*.kt' '*.json' | grep -vi "electroneum[^/]*$"); do
  grep -q "Foo" "$f" || echo "MISSING: $f"
done
grep -q '"name": "foo"' core/config-toggles/src/main/assets/configs/excluded_blockchains_config.json \
  || echo "MISSING: excluded_blockchains_config.json"
```

Empty output = every shared point is covered. Any `MISSING:` line is a forgotten edit point — fix it
or state why it does not apply. Then run the app with the network enabled in the Tester menu: icon,
rate, address, receive.