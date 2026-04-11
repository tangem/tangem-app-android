# WalletConnect Architecture

WalletConnect v2 integration across two module groups: `domain/wallet-connect` (contracts & models) and `data/wallet-connect` (implementation).

---

## Module Layout

```
domain/wallet-connect/          — contracts: interfaces, use cases, analytics
domain/wallet-connect/models/   — pure data models (JVM module, no Android dependency)
data/wallet-connect/            — implementation: SDK integration, signing, session management
```

### Build Dependencies

**domain/wallet-connect** (Android library):
- `domain/core`, `domain/models`, `domain/tokens/models`, `domain/wallets/models`
- `domain/walletConnect/models`, `domain/transaction`, `domain/transaction/models`
- `domain/blockaid/models`, `core/analytics`
- `tangemDeps.blockchain`, `tangemDeps.card.core`, `deps.moshi.adapters`

**domain/wallet-connect/models** (pure JVM):
- `domain/models`, `domain/wallets/models`, `domain/tokens/models`
- `domain/blockaid/models`, `domain/transaction/models`
- `deps.moshi` + KSP codegen, `deps.kotlin.serialization`

**data/wallet-connect** (Android library):
- All domain modules above + `domain/account`, `domain/account/status`, `domain/walletManager`
- `data/common`, `core/datasource`, `core/utils`, `core/analytics`
- `libs/blockchain-sdk`, `libs/crypto`
- `deps.reownCore`, `deps.reownWeb3` (Reown WalletConnect v2 SDK)
- `deps.arrow.core`, `deps.jodatime`, `deps.kotlin.coroutines`

---

## Domain Layer (`domain/wallet-connect`)

### Contracts (Interfaces & Use Cases)

| Interface / Class | Location | Purpose |
|---|---|---|
| `WalletConnectRepository` | `repository/` | `checkIsAvailable(userWalletId): Boolean` |
| `WcSessionsManager` | `repository/` | `sessions: Flow<Map<UserWallet, List<WcSession>>>`, `removeSession()`, `findSessionByTopic()` |
| `WcPairService` | root | `pairFlow: Flow<WcPairRequest>`, `pair(request)` — pair request channel |
| `WcRequestService` | root | `wcRequest: Flow<Pair<WcMethodName, WcSdkSessionRequest>>` — incoming dApp requests |
| `WcRequestUseCaseFactory` | root | `createUseCase<T>(request): Either<HandleMethodError, T>` — typed use case factory |
| `WcInitializeUseCase` | `usecase/initialize/` | `init(projectId)` — SDK bootstrap |
| `WcPairUseCase` | `usecase/pair/` | `invoke(): Flow<WcPairState>`, `approve()`, `reject()` + inner `Factory` |
| `WcSessionsUseCase` | `usecase/` | Convenience wrapper over `WcSessionsManager`: `invoke()`, `invokeSync()`, `findByTopic()` |
| `WcDisconnectUseCase` | `usecase/disconnect/` | `disconnect(session)`, `disconnect(topic)`, `disconnectAll()` |
| `CheckIsWalletConnectAvailableUseCase` | root | Wraps `WalletConnectRepository.checkIsAvailable` with `Either.catch` |

### Method Use Case Hierarchy

```
WcMethodUseCase                          — marker interface for all WC method handlers
├── WcSignUseCase<SignModel>             — sign flow: invoke() → Flow<WcSignState>, sign(), cancel()
│   ├── WcMessageSignUseCase             — personal_sign, eth_sign, eth_signTypedData, solana_signMessage
│   │   └── SignModel(humanMsg: String)
│   ├── WcTransactionUseCase             — eth_sendTransaction, eth_signTransaction, solana_signTransaction
│   │   └── SignModel = TransactionData
│   │   └── extends BlockAidTransactionCheck
│   └── WcListTransactionUseCase         — solana_signAllTransactions
│       └── SignModel = List<TransactionData>
│       └── extends BlockAidTransactionCheck
├── WcAddNetworkUseCase                  — wallet_addEthereumChain
│   └── invoke(): Either<HandleMethodError, AddNetwork>
│   └── approve(): Either<WcRequestError, String>
└── WcSwitchNetworkUseCase               — wallet_switchEthereumChain
    └── invoke(): Either<HandleMethodError, SwitchNetwork>
```

**Supporting interfaces:**
- `WcMethodContext` — shared context: `session`, `rawSdkRequest`, `network`, `method`, `derivationState`, `wallet`
- `WcMutableFee` — `dAppFee()`, `updateFee(fee)` (EVM send/sign transaction)
- `WcApproval` — `getAmount()`, `updateAmount()` (ERC-20 approval detection)
- `SignRequirements` — `isMultipleSignRequired()` (Solana large transactions)
- `BlockAidTransactionCheck` — `securityStatus: LceFlow<Throwable, Result>` with `Plain` / `Approval` subtypes
- `WcBlockAidEligibleTransactionUseCase` — `securityStatus: LceFlow<Throwable, CheckTransactionResult>`

### Sign State Machine

```kotlin
data class WcSignState<SignModel>(val signModel: SignModel, val domainStep: WcSignStep)

sealed interface WcSignStep {
    data object PreSign : WcSignStep           // initial, awaiting user action
    data object Signing : WcSignStep           // sign in progress (card tap / SDK call)
    data class Result(val result: Either<WcRequestError, String>) : WcSignStep  // terminal
}
```

### Pair State Machine

```kotlin
sealed interface WcPairState {
    data object Loading : WcPairState
    data class Error(val error: WcPairError) : WcPairState
    data class Proposal(val dAppSession: WcSessionProposal) : WcPairState
    sealed interface Approving : WcPairState {
        data class Loading(val session: WcSessionApprove) : Approving
        data class Result(val session: WcSessionApprove, val result: Either<WcPairError, WcAppMetaData>) : Approving
    }
}
```

### Models (`domain/wallet-connect/models`)

#### Core Session Models

| Model | Description |
|---|---|
| `WcSession` | Active session: `wallet`, `account` (CryptoPortfolio), `networks`, `sdkModel` (WcSdkSession), `securityStatus` (CheckDAppResult), `connectingTime`, `showWalletInfo` |
| `WcSessionDTO` | Persistence DTO: `topic`, `walletId`, `accountId`, `url`, `securityStatus`, `connectingTime`. Moshi-serialized |
| `WcPendingApprovalSessionDTO` | Pending approval: `pairingTopic`, `session` (WcSessionDTO with empty topic), `expiredTime` |
| `WcSessionProposal` | Pairing proposal: `dAppMetaData`, `proposalAccountNetwork: Map<AccountId, ProposalNetwork>`, `securityStatus` |
| `WcSessionApprove` | User-approved session: `wallet`, `account`, `network: List<Network>` |
| `WcPairRequest` | Pair trigger: `uri`, `source` (QR/DEEPLINK/CLIPBOARD/ETC), `userWalletId`, `screen?` |

#### SDK Copy Models (`sdkcopy/`)

Domain-safe copies of Reown SDK types (no SDK dependency in domain):

| Model | SDK Original |
|---|---|
| `WcSdkSession` | `Wallet.Model.Session` — `topic`, `appMetaData`, `namespaces` |
| `WcSdkSessionRequest` | `Wallet.Model.SessionRequest` — `topic`, `chainId`, `dAppMetaData`, `request` (JSONRPCRequest) |
| `WcSdkSessionProposal` | `Wallet.Model.SessionProposal` — `name`, `description`, `url`, `proposerPublicKey` |
| `WcAppMetaData` | `Core.Model.AppMetaData` — `name`, `description`, `url`, `icons`, `redirect`, `appLink`, `linkMode`, `verifyUrl` |

#### Method Models

| Model | Description |
|---|---|
| `WcMethod` | Sealed interface. Only subtype: `Unsupported(request)` |
| `WcMethodName` | Sealed interface with `raw: String`. Subtypes: `WcEthMethodName` (8 entries), `WcSolanaMethodName` (3 entries), `Unsupported` |
| `WcEthMethod` | Sealed interface extending `WcMethod`: `MessageSign`, `SignTypedData`, `SendTransaction`, `SignTransaction`, `AddEthereumChain`, `SwitchEthereumChain` |
| `WcSolanaMethod` | Sealed interface extending `WcMethod`: `SignMessage`, `SignTransaction`, `SignAllTransaction`. Has `methodName` and `trimmedPrefixMethodName` |
| `WcEthTransactionParams` | Moshi DTO: `from`, `to`, `data`, `gas`, `gasPrice`, `value`, `nonce` |
| `WcEthAddChain` | Moshi DTO: `chainId` (hex EIP-155) |
| `WcEthSignTypedDataParams` | Moshi DTO: `message` → `Message(contents)` |
| `WcApprovedAmount` | ERC-20 approval info: `amount: Amount?` (null = unlimited), `chainId`, `logoUrl` |

#### Error Models

| Error | Description |
|---|---|
| `WcPairError` | Sealed class with `code`/`message`. Subtypes: `UriAlreadyUsed`, `PairingFailed`, `InvalidDomainURL`, `UnsupportedDApp`, `UnsupportedBlockchains`, `InvalidConnectionRequest`, `ProposalExpired`, `ApprovalFailed`, `RejectionFailed`, `Unknown`, `TimeoutException` |
| `WcRequestError` | Sealed class. Subtypes: `WrappedSendError(SendTransactionError)`, `WcRespondError(code, message)`, `UnknownError(Throwable?)` |
| `HandleMethodError` | Extends `WcRequestError`. Subtypes: `Unsupported`, `UnknownSession`, `UnknownError`, `TangemUnsupportedNetwork`, `NotAddedNetwork`, `RequiredNetwork` |

### Analytics (`WcAnalyticEvents`)

Sealed class hierarchy under category `"Wallet Connect"`. Key events:

| Event | Trigger |
|---|---|
| `ScreenOpened` | WC screen displayed (AppsFlyer) |
| `NewPairInitiated` | Pair request started (source + screen) |
| `PairRequested` | dApp connection requested (name, URL, networks, verification) |
| `PairFailed` | Pair failed (error code + description) |
| `DAppConnected` | Session approved (AppsFlyer) |
| `DAppConnectionFailed` | SDK approval failed |
| `SessionDisconnected` | Session disconnected |
| `SignatureRequestReceived` | Request shown to user (emulation status, verification) |
| `SignatureRequestHandled` | Request signed successfully (AppsFlyer) |
| `SignatureRequestFailed` | Sign failed (error details) |
| `SignatureRequestReceivedFailed` | Use case creation failed |
| `ButtonSign` / `ButtonCancel` | User taps sign/cancel |
| `ButtonDisconnect` / `ButtonDisconnectAll` | Disconnect actions |
| `NoticeSecurityAlert` | Security warning shown (Domain/SmartContract source) |
| `SolanaLargeTransaction` / `SolanaLargeTransactionStatus` | Large Solana tx workaround |

---

## Data Layer (`data/wallet-connect`)

### Package Structure

```
com.tangem.data.walletconnect/
├── DefaultWalletConnectRepository.kt   — availability check (multi-currency wallets only)
├── di/
│   └── WalletConnectDataModule.kt      — Hilt @SingletonComponent DI wiring
├── initialize/
│   └── DefaultWcInitializeUseCase.kt   — SDK bootstrap (CoreClient + WalletKit init)
├── model/
│   ├── CAIP2.kt                        — chain identifier (e.g. "eip155:1")
│   ├── CAIP10.kt                       — account identifier (chain + address)
│   └── NamespaceKey.kt                 — inline value class for namespace key string
├── pair/
│   ├── DefaultWcPairService.kt         — pair request channel + deeplink filtering
│   ├── DefaultWcPairUseCase.kt         — full pairing flow (proposal → approve/reject)
│   ├── WcPairSdkDelegate.kt           — low-level SDK pair/approve/reject calls
│   ├── CaipNamespaceDelegate.kt       — builds CAIP namespace maps for session approval
│   ├── AssociateNetworksDelegate.kt    — maps dApp-requested chains → app accounts/networks
│   └── UnsupportedDApps.kt            — hardcoded blocklist of problematic dApps
├── request/
│   ├── DefaultWcRequestService.kt      — receives SDK session requests, routes to converters
│   ├── DefaultWcRequestUseCaseFactory.kt — creates typed WcMethodUseCase from raw request
│   └── WcRequestToUseCaseConverter.kt  — interface: raw request → method name + use case
├── respond/
│   ├── WcRespondService.kt            — interface for responding/rejecting to dApp
│   └── DefaultWcRespondService.kt     — WalletKit.respondSessionRequest wrapper + dedup cache
├── sessions/
│   └── DefaultWcSessionsManager.kt    — session lifecycle: list, associate, extend, disconnect
├── sign/
│   ├── BaseWcSignUseCase.kt           — abstract base for all sign/send use cases
│   ├── WcSignUseCaseDelegate.kt       — state machine: PreSign → Signing → Result
│   ├── SignStateConverter.kt          — WcSignState transition helpers
│   ├── BlockAidChainNameConverter.kt  — Network → BlockAid chain name mapping
│   └── WcMethodUseCaseContext.kt      — shared context (session, request, network, address)
├── network/ethereum/
│   ├── WcEthNetwork.kt                — EVM request converter + NamespaceConverter (eip155)
│   ├── WcEthMessageSignUseCase.kt     — personal_sign, eth_sign
│   ├── WcEthSignTypedDataUseCase.kt   — eth_signTypedData, eth_signTypedData_v4
│   ├── WcEthSendTransactionUseCase.kt — eth_sendTransaction (sign + broadcast)
│   ├── WcEthSignTransactionUseCase.kt — eth_signTransaction (sign only)
│   ├── WcEthAddNetworkUseCase.kt      — wallet_addEthereumChain + WcEthAddSwitchCommonDelegate
│   ├── WcEthSwitchNetworkUseCase.kt   — wallet_switchEthereumChain
│   └── WcEthTxHelper.kt              — transaction data builder + dApp fee + approval detection
├── network/solana/
│   ├── WcSolanaNetwork.kt             — Solana request converter + NamespaceConverter
│   ├── WcSolanaMessageSignUseCase.kt  — solana_signMessage
│   ├── WcSolanaSignTransactionUseCase.kt — solana_signTransaction (+ large tx workaround)
│   ├── WcSolanaSignAllTransactionUseCase.kt — solana_signAllTransactions
│   ├── SolanaBlockAidAddressConverter.kt — base58→base64 address conversion for BlockAid
│   └── Model.kt                       — Moshi request DTOs
└── utils/
    ├── WcSdkObserver.kt               — interface extending WalletKit.WalletDelegate (observer pattern)
    ├── WcNamespaceConverter.kt         — interface: CAIP2 → Blockchain → Network
    ├── WcNetworksConverter.kt          — resolves networks/addresses for WC sessions
    ├── WcExtensions.kt                — VerifyContext.getDappOriginUrl() extension
    ├── WcAppMetaDataConverter.kt       — Core.Model.AppMetaData → WcAppMetaData
    ├── WcSdkSessionConverter.kt        — Wallet.Model.Session → WcSdkSession
    ├── WcSdkSessionRequestConverter.kt — Wallet.Model.SessionRequest → WcSdkSessionRequest
    ├── JSONRPCRequestConverter.kt      — JSON-RPC request model converter
    └── BlockAidVerificationDelegate.kt — transaction security check via BlockAid API
```

### Core Flows

#### 1. Initialization

`DefaultWcInitializeUseCase.init(projectId)`:
1. Initializes `CoreClient` with relay server URL (`wss://relay.walletconnect.com`) and app metadata
2. Initializes `WalletKit` with `CoreClient`
3. Sets a `WalletDelegate` that fans out all SDK callbacks to registered `WcSdkObserver`s
4. Observers: `DefaultWcSessionsManager`, `DefaultWcRequestService`, `WcPairSdkDelegate`

#### 2. Pairing (New Session)

```
User scans QR / deeplink
    → DefaultWcPairService.pair(WcPairRequest)       // buffered Channel
    → DefaultWcPairUseCase.invoke()                   // Flow<WcPairState>
        → WcPairSdkDelegate.pair(uri)                 // WalletKit.pair + await proposal callback (15s timeout)
        → UnsupportedDApps check                      // blocklist: dydx, apex, dfx, sandbox, paradex
        → AssociateNetworksDelegate.associateAccounts  // maps CAIP chains → accounts & networks
        → BlockAidVerifier.verifyDApp                  // dApp security check
        → emit WcPairState.Proposal                    // UI shows proposal
        → await approve/reject from user               // Channel<TerminalAction>
        → CaipNamespaceDelegate.associate              // builds CAIP10 namespace map for all networks
        → WcPairSdkDelegate.approve                   // WalletKit.approveSession
        → WcPairSdkDelegate (onSessionSettleResponse)  // persists session via WalletConnectStore
```

#### 3. Session Management

`DefaultWcSessionsManager.sessions: Flow<Map<UserWallet, List<WcSession>>>`:
- Combines `GetWalletsUseCase`, `WalletConnectStore.sessions`, `MultiAccountListSupplier`
- Associates stored sessions (DTOs) with SDK active sessions and user wallets
- Merges pending approvals (for late `onSessionSettleResponse` callbacks)
- Auto-cleans orphaned sessions (in store but not in SDK, empty networks, unknown SDK sessions)
- Extends all active sessions on SDK init
- Listens for `onSessionDelete` callbacks to remove sessions from store

#### 4. Request Handling

```
dApp sends request
    → WalletKit.WalletDelegate.onSessionRequest       // SDK callback
    → DefaultWcRequestService.onSessionRequest         // WcSdkObserver
        → WcSdkSessionRequestConverter → WcSdkSessionRequest
        → requestConverters.toWcMethodName()           // WcEthNetwork or WcSolanaNetwork
        → unsupported methods: reject + skip (wallet_* prefixed are silently dropped)
        → dedup filter (SHA-256 of params, 120s TTL)
        → emit (WcMethodName, WcSdkSessionRequest)    // Channel → Flow
    → Feature layer collects wcRequest flow
    → DefaultWcRequestUseCaseFactory.createUseCase()
        → converter.toUseCase() → typed WcMethodUseCase
```

#### 5. Signing / Sending

All sign use cases extend `BaseWcSignUseCase` and use `WcSignUseCaseDelegate` as a state machine:

```
invoke() → channelFlow
    ├── emit WcSignState(model, PreSign)     // initial state
    ├── listen middleActions                  // fee updates, approval amount changes
    └── listen finalActions
        ├── Sign → onSign(state)             // abstract, per use case
        │   ├── sign/send via domain use case
        │   ├── respondService.respond()     // send result back to dApp
        │   └── emit WcSignState(Result)
        └── Cancel → onCancel()
            └── respondService.rejectRequestNonBlock()
```

**State transitions:** `PreSign ↔ middleAction updates → Signing → Result`
**Re-signing:** Flow stays open after Result to allow retry on error.

### Supported Methods

#### EVM (eip155)

| Method | Use Case | Action |
|---|---|---|
| `personal_sign` | `WcEthMessageSignUseCase` | Keccak hash with ETH prefix + sign + unmarshal RSV |
| `eth_sign` | `WcEthMessageSignUseCase` | Same as personal_sign (swapped param order) |
| `eth_signTypedData` | `WcEthSignTypedDataUseCase` | EIP-712 typed data hash + sign |
| `eth_signTypedData_v4` | `WcEthSignTypedDataUseCase` | EIP-712 typed data hash + sign |
| `eth_sendTransaction` | `WcEthSendTransactionUseCase` | Build `TransactionData.Uncompiled` + `SendTransactionUseCase` + respond tx hash |
| `eth_signTransaction` | `WcEthSignTransactionUseCase` | Build `TransactionData.Uncompiled` + `PrepareForSendUseCase` + respond signed hex |
| `wallet_addEthereumChain` | `WcEthAddNetworkUseCase` | Validate chain → `WalletKit.updateSession` with new namespace |
| `wallet_switchEthereumChain` | `WcEthSwitchNetworkUseCase` | Validate chain exists in portfolio |

**EVM-specific features:**
- `WcEthTxHelper` — builds `TransactionData.Uncompiled` from `WcEthTransactionParams`, extracts dApp fee (gas/gasPrice), detects ERC-20 approval via BlockAid simulation
- `WcEthTxAction` — sealed interface for middle actions: `UpdateFee`, `UpdateApprovalAmount`
- `WcEthAddSwitchCommonDelegate` — shared logic for add/switch chain (hex chainId → CAIP2, validation)

#### Solana

| Method | Use Case | Action |
|---|---|---|
| `solana_signMessage` | `WcSolanaMessageSignUseCase` | Decode base58 + sign + respond base58 signature |
| `solana_signTransaction` | `WcSolanaSignTransactionUseCase` | Decode base64 + `PrepareAndSignUseCase` + respond base58 signature. Large tx workaround: if > threshold, uses `SendLargeSolanaTransactionUseCase` for NFC cards |
| `solana_signAllTransactions` | `WcSolanaSignAllTransactionUseCase` | Decode base64 batch + `PrepareForSendUseCase` + respond JSON `{ transactions: [...] }` base64 |

**Solana-specific features:**
- `SolanaBlockAidAddressConverter` — converts base58 address to base64 for BlockAid API
- Mainnet chain IDs: `5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp`, `4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ`

### Key Patterns

#### WcSdkObserver (Observer Pattern)
`WcSdkObserver` extends `WalletKit.WalletDelegate` with default no-op implementations. Three observers:
- **WcPairSdkDelegate** — `onSessionProposal`, `onSessionSettleResponse`, `onError`
- **DefaultWcRequestService** — `onSessionRequest`
- **DefaultWcSessionsManager** — `onSessionDelete`

#### WcRequestToUseCaseConverter (Strategy Pattern)
Each network implements this interface:
- `toWcMethodName(request): WcMethodName?` — identifies if this converter handles the request
- `toUseCase(request): Either<HandleMethodError, WcMethodUseCase>` — creates the typed use case

`DiHelperBox` aggregates converters (`WcEthNetwork` + `WcSolanaNetwork`).

#### WcNamespaceConverter (Chain Resolution)
Per-network converter: CAIP-2 chain ID → `Blockchain` enum → `Network` domain model. Uses `NetworkFactory` from `data/common`. Implementations:
- `WcEthNetwork.NamespaceConverter` — namespace `eip155`, uses `Blockchain.fromChainId(int)`
- `WcSolanaNetwork.NamespaceConverter` — namespace `solana`, hardcoded mainnet/testnet chain IDs

#### AssistedInject Factories
All method use cases use `@AssistedInject` with inner `@AssistedFactory` interfaces. Assisted params: `context: WcMethodUseCaseContext` + `method`. Normal injection: services, analytics, helpers.

#### BlockAid Security
Two verification levels:
1. **dApp verification** — during pairing (`BlockAidVerifier.verifyDApp` → `CheckDAppResult`: SAFE/UNSAFE/FAILED_TO_VERIFY)
2. **Transaction verification** — during signing (`BlockAidVerificationDelegate.getSecurityStatus` → `LceFlow<Throwable, CheckTransactionResult>`). Supports EVM params and Solana transaction params (not solana_signMessage).

### Persistence

`WalletConnectStore` (from `core/datasource`) stores:
- `sessions: Flow<Set<WcSessionDTO>>` — active sessions
- `pendingApproval: Flow<Set<WcPendingApprovalSessionDTO>>` — sessions awaiting SDK settlement

Sessions are reconciled with `WalletKit.getListOfActiveSessions()` on every emission.

### DI Structure

All bindings in `WalletConnectDataModule` (`@SingletonComponent`, `@Singleton` scope):
- Domain interfaces → default implementations
- `DiHelperBox` wraps `Set<WcRequestToUseCaseConverter>` (workaround for Hilt multibinding)
- `Set<WcNamespaceConverter>` assembled from per-network converters
- Use case factories via `@AssistedFactory`

---

## Adding a New Blockchain

1. **Domain models** — add `Wc{Chain}Method` sealed interface in `domain/wallet-connect/models`, add `Wc{Chain}MethodName` enum implementing `WcMethodName`
2. **Network converter** — create `network/{chain}/Wc{Chain}Network.kt` implementing `WcRequestToUseCaseConverter`. Add inner `NamespaceConverter` implementing `WcNamespaceConverter`
3. **Method use cases** — create per-method use cases extending `BaseWcSignUseCase`
4. **Factories** — create inner `Factories` class with `@AssistedFactory` references
5. **DI registration** in `WalletConnectDataModule`:
   - Provide the network class and its `NamespaceConverter`
   - Add to `DiHelperBox.handlers` set
   - Add `NamespaceConverter` to the `namespaceConverters` set

---

## TON Blockchain WalletConnect Support

Reference: https://docs.walletconnect.network/wallet-sdk/chain-support/ton

### TON in Tangem Codebase (Existing Support)

TON is already fully supported in the app — just not wired into WalletConnect:

- **Blockchain enum:** `Blockchain.TON` (mainnet), `Blockchain.TONTestnet` (testnet)
- **Network ID:** `"the-open-network"` / `"the-open-network/test"`
- **Coin ID:** `"the-open-network"`
- **Elliptic curve:** `Ed25519Slip0010` (EdDSA)
- **Derivation:** Supported via `AccountNodeRecognizer`
- **Staking:** Integrated via StakeKit (`ton-ton-chorus-one-pools-staking`)
- **Providers:** TonCentral, NowNodes, GetBlock
- **NOT in ExcludedBlockchains** — fully enabled

### TON WalletConnect Specification

#### Namespace & Chain IDs

| Chain | CAIP-2 Identifier | Chain ID |
|---|---|---|
| TON Mainnet | `ton:-239` | `-239` |
| TON Testnet | `ton:-3` | `-3` |

**Namespace key:** `"ton"`

#### Session Properties (Required)

TON requires session properties when approving a session. The `Wallet.Params.SessionApprove` in Reown SDK supports a `properties: Map<String, String>?` parameter:

```kotlin
Wallet.Params.SessionApprove(
    proposerPublicKey = ...,
    namespaces = ...,
    properties = mapOf(
        "ton_getPublicKey" to publicKeyHex,     // Ed25519 public key, hex-encoded
        "ton_getStateInit" to stateInitBase64,   // StateInit of wallet contract, base64-encoded BoC
    ),
)
```

These enable dApps to verify signatures and compute wallet addresses without additional requests.

**Impact:** `DefaultWcPairUseCase.walletKitApproveSession()` and `CaipNamespaceDelegate` must be extended to populate session properties when TON namespaces are present.

#### Supported RPC Methods

##### 1. `ton_sendMessage`

Submits transaction messages to the TON network.

**Request params:**
```json
{
  "valid_until": 1234567890,        // optional, UNIX timestamp
  "from": "UQ...",                  // optional, sender address (TEP-123 format)
  "messages": [
    {
      "address": "UQ...",           // recipient (TEP-123 format)
      "amount": "1000000000",       // value in nanotons (string or number)
      "payload": "te6c...",         // optional, base64 BoC
      "stateInit": "te6c..."        // optional, base64 BoC
    }
  ]
}
```

**Response:** Base64-encoded signed transaction (BoC) on success.

**Notes:**
- Multiple messages in a single request (TON supports multi-message transactions)
- `amount` is in nanotons (1 TON = 10^9 nanotons)
- `payload` contains the cell payload (smart contract call data)
- Unlike EVM, TON transactions can contain multiple output messages

##### 2. `ton_signData`

Signs off-chain payloads for authentication or verification.

**Request params** (3 variants):
```json
// Text signing
{ "type": "text", "text": "Hello", "from": "UQ..." }

// Binary signing
{ "type": "binary", "bytes": "<base64-encoded>", "from": "UQ..." }

// Cell signing
{ "type": "cell", "schema": "<TL-B schema>", "cell": "<base64 BoC>", "from": "UQ..." }
```

**Response:** `{ signature: "<base64>", address, timestamp, domain, payload }`

**Notes:**
- `from` is optional — omission means wallet selects the address
- Uses Ed25519 signature on original bytes
- Text variant likely most common (dApp authentication)

#### Address Format

TON uses **TEP-123** address format (e.g., `UQ...` or `EQ...` for bounceable/non-bounceable). This is different from raw hex addresses used by EVM chains. The `WcNetworksConverter.getAddressForWC()` may need special handling for TON address format.

### Implementation Plan

#### 1. Domain Models (`domain/wallet-connect/models`)

**New file: `WcTonMethod.kt`**
```kotlin
sealed interface WcTonMethod : WcMethod {
    data class SendMessage(
        val validUntil: Long?,
        val from: String?,
        val messages: List<TonTransactionMessage>,
    ) : WcTonMethod

    data class SignData(
        val type: SignDataType,
        val from: String?,
    ) : WcTonMethod
}

data class TonTransactionMessage(
    val address: String,
    val amount: String,
    val payload: String?,
    val stateInit: String?,
)

sealed interface SignDataType {
    data class Text(val text: String) : SignDataType
    data class Binary(val bytes: String) : SignDataType
    data class Cell(val schema: String, val cell: String) : SignDataType
}
```

**Add to `WcMethodName.kt`:**
```kotlin
enum class WcTonMethodName(override val raw: String) : WcMethodName {
    SendMessage("ton_sendMessage"),
    SignData("ton_signData"),
}
```

**New file: Moshi request DTOs** (with `@JsonClass(generateAdapter = true)`)

#### 2. Network Converter (`data/wallet-connect/network/ton/WcTonNetwork.kt`)

```kotlin
internal class WcTonNetwork(
    private val moshi: Moshi,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
    private val networksConverter: WcNetworksConverter,
) : WcRequestToUseCaseConverter {

    override fun toWcMethodName(request: WcSdkSessionRequest): WcTonMethodName? {
        return WcTonMethodName.entries.find { it.raw == request.request.method }
    }

    override suspend fun toUseCase(request: WcSdkSessionRequest): Either<HandleMethodError, WcMethodUseCase> {
        // Similar pattern to WcEthNetwork / WcSolanaNetwork
    }

    internal class NamespaceConverter(
        override val excludedBlockchains: ExcludedBlockchains,
    ) : WcNamespaceConverter {
        override val namespaceKey = NamespaceKey("ton")

        override fun toBlockchain(chainId: CAIP2): Blockchain? {
            if (chainId.namespace != namespaceKey.key) return null
            return when (chainId.reference) {
                "-239" -> Blockchain.TON
                "-3" -> Blockchain.TONTestnet
                else -> null
            }
        }
    }

    internal class Factories @Inject constructor(
        val sendMessage: WcTonSendMessageUseCase.Factory,
        val signData: WcTonSignDataUseCase.Factory,
    )
}
```

#### 3. Method Use Cases

**`WcTonSendMessageUseCase`** — extends `BaseWcSignUseCase`:
- Build TON transaction from `TonTransactionMessage` list
- Sign via `PrepareAndSignUseCase` or TON-specific signing
- Respond with base64-encoded signed BoC
- Note: TON multi-message transactions are a single sign operation

**`WcTonSignDataUseCase`** — extends `BaseWcSignUseCase`:
- Implements `WcMessageSignUseCase` (similar to personal_sign)
- Prepare bytes based on `SignDataType` (text/binary/cell)
- Sign with Ed25519 via `SignUseCase`
- Respond with base64 signature + metadata

#### 4. Session Properties

**Modify `DefaultWcPairUseCase.walletKitApproveSession()`:**
```kotlin
// After building namespaces, check if TON is present
val tonProperties = if (namespaces.containsKey("ton")) {
    // Get public key and stateInit from WalletManager
    val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, tonNetwork)
    mapOf(
        "ton_getPublicKey" to walletManager.wallet.publicKey.blockchainKey.toHexString(),
        "ton_getStateInit" to getStateInitBase64(walletManager),  // needs implementation
    )
} else null

val sessionApprove = Wallet.Params.SessionApprove(
    proposerPublicKey = sdkSessionProposal.proposerPublicKey,
    namespaces = namespaces,
    properties = tonProperties,  // NEW: was not set before
)
```

**Impact on existing code:**
- `CaipNamespaceDelegate` — no changes needed (already generic)
- `WcPairSdkDelegate.approve()` — no changes needed (passes `SessionApprove` as-is)
- `DefaultWcPairUseCase` — needs to pass `properties` to `SessionApprove`

#### 5. Address Handling

TON uses TEP-123 format. Check if `WalletManagersFacade.getDefaultAddress()` returns the correct format for TON. If it returns raw hex, add TON-specific handling in `WcNetworksConverter.getAddressForWC()`:

```kotlin
suspend fun getAddressForWC(userWalletId: UserWalletId, network: Network): String? {
    return when (network.toBlockchain()) {
        Blockchain.XDC, Blockchain.XDCTestnet -> // existing legacy address logic
        Blockchain.TON, Blockchain.TONTestnet -> // may need TEP-123 format check
        else -> walletManagersFacade.getDefaultAddress(userWalletId, network)
    }
}
```

#### 6. BlockAid Support

Add TON chain name to `BlockAidChainNameConverter`:
```kotlin
Blockchain.TON -> "ton"  // verify with BlockAid docs
```

If BlockAid doesn't support TON yet, `ton_sendMessage` and `ton_signData` use cases should return failed-to-validate result (same pattern as Solana `solana_signMessage`).

#### 7. DI Registration (`WalletConnectDataModule`)

```kotlin
// Add provider for WcTonNetwork
@Provides @Singleton
fun wcTonNetwork(...): WcTonNetwork = WcTonNetwork(...)

// Add provider for NamespaceConverter
@Provides @Singleton
fun wcTonNetworkNamespaceConverter(excludedBlockchains: ExcludedBlockchains): WcTonNetwork.NamespaceConverter =
    WcTonNetwork.NamespaceConverter(excludedBlockchains)

// Update DiHelperBox
fun diHelperBox(ethNetwork: WcEthNetwork, solanaNetwork: WcSolanaNetwork, tonNetwork: WcTonNetwork) =
    DiHelperBox(handlers = setOf(ethNetwork, solanaNetwork, tonNetwork))

// Update namespaceConverters set
fun namespaceConverters(
    ethNamespaceConverter: WcEthNetwork.NamespaceConverter,
    solanaNamespaceConverter: WcSolanaNetwork.NamespaceConverter,
    tonNamespaceConverter: WcTonNetwork.NamespaceConverter,
): Set<WcNamespaceConverter> = setOf(ethNamespaceConverter, solanaNamespaceConverter, tonNamespaceConverter)
```

### Files to Create

| File | Module | Description |
|---|---|---|
| `WcTonMethod.kt` | `domain/wallet-connect/models` | Sealed interface + data classes |
| `WcTonMethodName` entries | `domain/wallet-connect/models` | Add enum to `WcMethodName.kt` |
| `WcTonNetwork.kt` | `data/wallet-connect` | Request converter + namespace converter |
| `WcTonSendMessageUseCase.kt` | `data/wallet-connect` | ton_sendMessage handler |
| `WcTonSignDataUseCase.kt` | `data/wallet-connect` | ton_signData handler |
| `Model.kt` (TON DTOs) | `data/wallet-connect/network/ton/` | Moshi request/response DTOs |

### Files to Modify

| File | Change |
|---|---|
| `WalletConnectDataModule.kt` | Add TON providers, update `DiHelperBox`, update `namespaceConverters` |
| `DefaultWcPairUseCase.kt` | Pass `properties` to `Wallet.Params.SessionApprove` for TON sessions |
| `BlockAidChainNameConverter.kt` | Add `Blockchain.TON` mapping (if BlockAid supports it) |
| `WcNetworksConverter.kt` | TON address format handling in `getAddressForWC()` (if needed) |

### Key Differences from EVM/Solana

| Aspect | EVM | Solana | TON |
|---|---|---|---|
| Namespace | `eip155` | `solana` | `ton` |
| Chain ID format | Integer (1, 137...) | Hash string | Negative integer (-239, -3) |
| Address format | Hex (0x...) | Base58 | TEP-123 (UQ.../EQ...) |
| Signing curve | secp256k1 | Ed25519 | Ed25519 |
| Multi-message | No | `signAllTransactions` | Native (single `ton_sendMessage`) |
| Session properties | None | None | Required (`ton_getPublicKey`, `ton_getStateInit`) |
| Off-chain signing | `personal_sign`, `eth_signTypedData` | `solana_signMessage` | `ton_signData` (text/binary/cell) |
| Transaction format | RLP / hex | Base64 compiled | Base64 BoC (Bag of Cells) |