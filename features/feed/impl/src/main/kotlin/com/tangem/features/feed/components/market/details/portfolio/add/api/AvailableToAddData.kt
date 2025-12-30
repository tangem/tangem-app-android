package com.tangem.features.feed.components.market.details.portfolio.add.api

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

internal data class AvailableToAddData(
    val availableToAddWallets: Map<UserWalletId, AvailableToAddWallet>,
) {
    val isAvailableToAdd: Boolean
        get() = availableToAddWallets.isNotEmpty()
    val isSinglePortfolio: Boolean
        get() = availableToAddWallets.size == 1 && availableToAddWallets.values.first().accounts.size == 1
}

internal data class AvailableToAddWallet(
    val userWallet: UserWallet,
    val accounts: List<AccountStatus>,
    val availableNetworks: Set<TokenMarketInfo.Network>,
    val availableToAddAccounts: Map<AccountId, AvailableToAddAccount>,
)

@Serializable
internal data class AvailableToAddAccount(
    val account: AccountStatus,
    val availableNetworks: Set<TokenMarketInfo.Network>,
    val addedNetworks: Set<Network>,
) {
    val isSingleNetwork: Boolean
        get() = availableNetworks.size == 1

    val availableToAddNetworks: Set<TokenMarketInfo.Network> = availableNetworks
        .filter { available -> addedNetworks.none { added -> added.backendId == available.networkId } }
        .toSet()

    val addedMarketNetworks: Set<TokenMarketInfo.Network> = availableNetworks
        .filter { available -> addedNetworks.any { added -> added.backendId == available.networkId } }
        .toSet()
}

@Serializable
internal data class SelectedPortfolio(
    val userWallet: UserWallet,
    val account: AvailableToAddAccount,
    val isAccountMode: Boolean,
    val isAvailableMorePortfolio: Boolean,
)

internal data class SelectedNetwork(
    val selectedNetwork: TokenMarketInfo.Network,
    val cryptoCurrency: CryptoCurrency,
    val isAvailableMoreNetwork: Boolean,
)