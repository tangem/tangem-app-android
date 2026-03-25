# Domain Components

Key domain mechanisms that orchestrate data flow: suppliers, fetchers, and use cases.

## Retrieving Core Models

### UserWallet

#### UserWalletsListRepository

**Location:** `domain/common` — `com.tangem.domain.common.wallets.UserWalletsListRepository`

Repository for managing user wallets list. Provides `StateFlow<List<UserWallet>?>` for the wallets list and `StateFlow<UserWallet?>` for the selected wallet. Supports loading, selecting, saving, locking/unlocking (biometric, access code), deleting, and reordering wallets.

### Account / AccountList

#### SingleAccountSupplier

**Location:** `domain/account` — `com.tangem.domain.account.supplier.SingleAccountSupplier`

Supplier that provides a single `Account` by `AccountId`. Has convenience methods `filterPaymentAccount` and `filterCryptoPortfolioAccount` to filter by account subtype.

#### SingleAccountListSupplier

**Location:** `domain/account` — `com.tangem.domain.account.supplier.SingleAccountListSupplier`

Supplier that provides an `AccountList` for a specific user wallet by `UserWalletId`.

#### MultiAccountListSupplier

**Location:** `domain/account` — `com.tangem.domain.account.supplier.MultiAccountListSupplier`

Supplier that provides a list of `AccountList`s for all user wallets. Extends `FlowCachingSupplier`.

#### SingleAccountListFetcher

**Location:** `domain/account` — `com.tangem.domain.account.fetcher.SingleAccountListFetcher`

Fetcher that fetches a list of accounts for a single wallet by `UserWalletId`. Extends `FlowFetcher`.

### AccountStatus / AccountStatusList

#### SingleAccountStatusSupplier

**Location:** `domain/account/status` — `com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier`

Supplier that provides a single `AccountStatus` by account identifier. Extends `FlowCachingSupplier`.

#### SingleAccountStatusListSupplier

**Location:** `domain/account/status` — `com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier`

Same as `SingleAccountListSupplier` but provides `AccountStatusList` (accounts with balances) for a specific user wallet.

#### MultiAccountStatusListSupplier

**Location:** `domain/account/status` — `com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier`

Same as `MultiAccountListSupplier` but provides a list of `AccountStatusList`s for all user wallets.

### Network / NetworkStatus

#### SingleNetworkStatusSupplier

**Location:** `domain/networks` — `com.tangem.domain.networks.single.SingleNetworkStatusSupplier`

Supplier of `NetworkStatus` for a specific network and wallet. Extends `FlowCachingSupplier`.

#### MultiNetworkStatusSupplier

**Location:** `domain/networks` — `com.tangem.domain.networks.multi.MultiNetworkStatusSupplier`

Supplier of all `NetworkStatus`es (as `Set<NetworkStatus>`) for a selected wallet. Extends `FlowCachingSupplier`.

#### SingleNetworkStatusFetcher

**Location:** `domain/networks` — `com.tangem.domain.networks.single.SingleNetworkStatusFetcher`

Fetcher of network status for a single `Network` by `UserWalletId`. Extends `FlowFetcher`.

#### MultiNetworkStatusFetcher

**Location:** `domain/networks` — `com.tangem.domain.networks.multi.MultiNetworkStatusFetcher`

Fetcher of network statuses for a set of `Network`s for a multi-currency wallet by `UserWalletId`. Extends `FlowFetcher`.

## Updating Balances

### WalletBalanceFetcher

**Location:** `domain/tokens` — `com.tangem.domain.tokens.wallet.WalletBalanceFetcher`

Fetcher of wallet balances by `UserWalletId`. Selects the appropriate fetching strategy based on wallet type (multi-wallet, single wallet with tokens, single wallet). Delegates to `BalanceFetchingOperations` for shared fetching logic.

### CryptoCurrencyBalanceFetcher

**Location:** `domain/account/status` — `com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher`

Fetches and refreshes balances for specific crypto currencies. Uses per-wallet mutexes to allow concurrent refreshes for different wallets while preventing concurrent refreshes for the same wallet. Delegates to `BalanceFetchingOperations`.

## Managing Portfolio (User Tokens)

### ManageCryptoCurrenciesUseCase

**Location:** `domain/account/status` — `com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase`

Use case for adding and removing crypto currencies in an account.