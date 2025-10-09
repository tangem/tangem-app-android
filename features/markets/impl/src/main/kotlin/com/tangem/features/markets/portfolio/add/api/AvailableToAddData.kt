package com.tangem.features.markets.portfolio.add.api

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

data class AvailableToAddData(
    val availableToAddWallets: Map<UserWalletId, AvailableToAddWallet>,
) {
    val isSinglePortfolio: Boolean
        get() = availableToAddWallets.size == 1 && availableToAddWallets.values.first().accounts.size <= 1
}

data class AvailableToAddWallet(
    val userWallet: UserWallet,
    val accounts: Set<AccountStatus>,
    val availableNetworks: Set<TokenMarketInfo.Network>,
    val availableToAddAccounts: Map<AccountId, AvailableToAddAccount>,
) {
    val isSingleAvailableNetworks: Boolean get() = availableNetworks.size <= 1
    val singleAvailableNetworks: TokenMarketInfo.Network get() = availableNetworks.first()
}

data class AvailableToAddAccount(
    val account: AccountStatus,
    val availableNetworks: Set<TokenMarketInfo.Network>,
    val availableToAddNetworks: Set<TokenMarketInfo.Network>,
)

data class SelectedPortfolio(
    val userWallet: UserWallet,
    val account: AccountStatus,
    val isAccountMode: Boolean,
    val availableMorePortfolio: Boolean,
)

data class SelectedNetwork(
    val selectedNetwork: TokenMarketInfo.Network,
    val cryptoCurrency: CryptoCurrency,
    val availableMoreNetwork: Boolean,
)