# Domain Models

Core business models used across the application. Models are defined in `domain/models/` and `domain/account/`.

## StatusSource

**Location:** `domain/models` — `com.tangem.domain.models.StatusSource`

Enum representing data loading/refresh status. Used across all status models (NetworkStatus, QuoteStatus, YieldBalance, CryptoCurrencyStatus.Sources):
- `CACHE` — initial status, data loaded from cache
- `ACTUAL` — terminal status, data successfully fetched from server
- `ONLY_CACHE` — terminal status, data could not be refreshed (only cached data available)

## CryptoCurrency

**Location:** `domain/models` — `com.tangem.domain.models.currency.CryptoCurrency`

Sealed class representing a cryptocurrency — either a native coin (`Coin`) or a token (`Token`). Used throughout the application: portfolio, token search, swaps, buy/sell, staking, etc.

## CryptoCurrencyStatus

**Location:** `domain/models` — `com.tangem.domain.models.currency.CryptoCurrencyStatus`

Model representing a currency with its balance state. Primarily used to display user's coin balance in the portfolio. Wraps `CryptoCurrency` with a `Value` sealed interface:

| Value subtype | Description |
|---|---|
| `Loading` | First-time fetch; once data is loaded, subsequent updates use cache via StatusSource, bypassing Loading |
| `Loaded` | Full data available |
| `Custom` | Custom token in portfolio; some data may be missing (e.g., no balance if backend has no quotes for it) |
| `NoQuote` | Balance known, no price data |
| `NoAccount` | Account not created (e.g., Solana reserve) |
| `Unreachable` | Network error |
| `NoAmount` | Coin is added to portfolio but no blockchain data available for it |
| `MissedDerivation` | Coin has no derivations — failed to obtain a blockchain network address |

All Value subtypes carry `sources: Sources` tracking data freshness per dimension: `networkSource`, `quoteSource`, `stakingBalanceSource`, and aggregated `total`.

## Network

**Location:** `domain/models` — `com.tangem.domain.models.network.Network`

Represents a blockchain network (e.g., Ethereum, Bitcoin). Contains network metadata: ID, name, currency symbol, derivation path, standard type (ERC20, TRC20, BEP20, etc.), and capabilities (token support, transaction extras, name resolving).

## NetworkStatus

**Location:** `domain/models` — `com.tangem.domain.models.network.NetworkStatus`

Blockchain balances for all tokens of a network at a specific address. Only `Verified` and `NoAccount` are cached.

| Value subtype | Description |
|---|---|
| `Verified` | Successful response from blockchain |
| `Unreachable` | Failed response from blockchain |
| `NoAccount` | Blockchain-specific status for chains that require a deposit to an address before it can be used |
| `MissedDerivation` | Derivation failed — no blockchain network address |

## QuoteStatus

**Location:** `domain/models` — `com.tangem.domain.models.quote.QuoteStatus`

Exchange rate between the app's selected fiat currency and a coin's currency.

## YieldBalance

**Location:** `domain/models` — `com.tangem.domain.models.staking.YieldBalance`

Staking yield balance for a specific `StakingID` (integrationId + address).

## TotalFiatBalance

**Location:** `domain/models` — `com.tangem.domain.models.TotalFiatBalance`

Aggregate fiat balance across all tokens. Sealed interface with three states: `Loading`, `Failed`, `Loaded(amount, source)`.

## TokenList

**Location:** `domain/models` — `com.tangem.domain.models.tokenlist.TokenList`

List of cryptocurrency tokens for display in portfolio. Sealed interface with subtypes:
- `GroupedByNetwork` — tokens grouped by `Network`, each group contains a list of `CryptoCurrencyStatus`
- `Ungrouped` — flat list of `CryptoCurrencyStatus`
- `Empty` — no tokens

All subtypes carry `totalFiatBalance: TotalFiatBalance` and `sortedBy: TokensSortType`.

## Account

**Location:** `domain/models` — `com.tangem.domain.models.account.Account`

Model representing a user account. Subtypes:
- **`Account.CryptoPortfolio`** — crypto portfolio with coins. All tokens in the account share the account's derivation (main account is an exception). Has a `DerivationIndex`: `0` for main account, `1..19` for secondary
- **`Account.Payment`** — account for Visa card integration

## AccountStatus

**Location:** `domain/models` — `com.tangem.domain.models.account.AccountStatus`

Model representing an account with balances. Has a similar structure to `Account`: `CryptoPortfolio` and `Payment` subtypes.

## AccountList

**Location:** `domain/account` — `com.tangem.domain.account.models.AccountList`

List of all accounts for a user wallet (`UserWallet`).

Business rules (enforced by factory returning `Either<Error, AccountList>`):
- Accounts list cannot be empty
- Exactly 1 main account
- Max 20 active accounts (`MAX_ACCOUNTS_COUNT`), max 1000 archived
- No duplicate AccountIds or custom AccountNames
- `totalAccounts >= activeAccounts`

## AccountStatusList

**Location:** `domain/account` — `com.tangem.domain.account.models.AccountStatusList`

Same as `AccountList` but with balances (wraps `AccountStatus` instead of `Account`).

## UserWallet

**Location:** `domain/models` — `com.tangem.domain.models.wallet.UserWallet`

Top-level model representing a user's wallet stored in the app. Subtypes:
- **`Cold`** — wallet backed by a physical Tangem card (NFC). Contains `ScanResponse`, card info, backup state
- **`Hot`** — software (hot) wallet without a physical card

## Model Hierarchy

```
UserWallet
 └─ AccountList / AccountStatusList
     └─ Account.CryptoPortfolio / AccountStatus.CryptoPortfolio
         ├─ CryptoCurrency (Coin | Token)
         │   └─ CryptoCurrencyStatus (currency + Value state)
         │       ├─ built from NetworkStatus (per network)
         │       ├─ built from QuoteStatus (per rawCurrencyId)
         │       └─ built from YieldBalance (per stakingId)
         ├─ AccountId (SHA-256 hash)
         ├─ DerivationIndex (0 = main)
         └─ CryptoPortfolioIcon (icon + color)
```