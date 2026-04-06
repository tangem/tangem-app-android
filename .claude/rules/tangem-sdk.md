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