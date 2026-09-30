# Tangem SDK & Libraries

## In-house SDKs (via `tangem_dependencies.toml`)

- **Blockchain SDK** (`com.tangem:blockchain`) — multichain SDK for working with blockchains: creating/signing transactions, fetching balances, managing addresses. Wrapped in `libs/blockchain-sdk/`
- **Card SDK** (`com.tangem.tangem-sdk-kotlin:core`, `:android`) — SDK for interacting with physical Tangem cards via NFC: scanning, wallet creation, key derivation, passcode management, backup. Wrapped in `libs/tangem-sdk-api/`
- **Hot SDK** (`com.tangem.tangem-hot-sdk-kotlin:core`, `:android`) — SDK for hot (software) wallets
- **Vico** (`com.tangem.vico`) — forked charting library Vico, adapted for project needs

## Wrapper Modules (`libs/`)

- `libs/blockchain-sdk/` — wrapper around Blockchain SDK, provides domain-level abstractions for blockchain operations
- `libs/tangem-sdk-api/` — wrapper around Card SDK, exposes NFC card interaction API to the app
- `libs/crypto/` — cryptographic utilities: derivation, address handling, blockchain-specific helpers
- `libs/auth/` — API key provider interfaces for external services (Express, StakeKit)
- `libs/visa/` — Visa integration: smart contracts, limits, balances via Web3j

## Private third-party SDKs

- **MeaWallet MPP** (`com.meawallet:mpp-test` / `mpp-prod`, plus TapAndPay `com.google.android.gms:play-services-tapandpay`, which MPP wraps and which is not on Google Maven) — Google Wallet push provisioning for the Tangem Pay card, published with their original POMs to the private GitHub Packages registry of `tangem-developments/tangem-app-android`. Their SHA-256 checksums are pinned in `gradle/verification-metadata.xml`: after a version bump run `./gradlew -Pmeawallet.sdk=impl --write-verification-metadata sha256 help` (the flag makes a token without access fail instead of silently writing nothing) and delete the old version's entries. Only `spend/push-provisioning/impl` touches the SDK, behind `GoogleWalletProvisioner` from the `api` module; without the SDK `:app` takes `spend/push-provisioning/no-op`. Mocked builds always take `spend/push-provisioning/mock`, a fake driven by the `uitest.tangempay.google_wallet_add_result` / `google_wallet_card_status` system properties, so UI tests never need the SDK.
  - The `meawallet-sdk` settings plugin (`plugins/settings`, applied in `settings.gradle.kts`) declares the registry and includes `impl` when `tangem-app-config` has `meawallet/mea_config` and the registry does not refuse the token: a HEAD for the `mpp-prod` `maven-metadata.xml` with `gpr.key` or `GITHUB_TOKEN`. No token, 401, 403 or 404 give `:app` the `no-op` module; any other answer (network error, 5xx) and `--offline` keep `impl`. `meawallet.sdk=impl|noop` (`no-op` works too) as `-P` or in `local.properties` skips the check, and `-P` wins; with SDK access, `meawallet.sdk=impl` in `local.properties` also saves the registry round-trip on every build. `REQUIRE_MEAWALLET_SDK=true` (publish workflows) turns the no-op into a configuration error.
  - Debug uses the `-debug` artifact, internal and external use `mpp-test`, release uses `mpp-prod`. `GoogleWalletProvisioner.environment` follows the linked artifact (`MeaPushProvisioning.Configuration.versionName()`), not the build type. The `-debug` artifact logs request bodies, card secret included, to Logcat.
  - The SDK handles activity request codes 1001–1005, so other `startActivityForResult` callers must not use them.
  - The impl manifest removes the SDK's `ACCESS_WIFI_STATE` permission (the SDK never touches Wi-Fi) and its Samsung Pay `<queries>` entry; recheck both after a version bump.
  - `MppPaymentNetwork` links against the optional Samsung Pay SDK, so JVM unit tests must not load it