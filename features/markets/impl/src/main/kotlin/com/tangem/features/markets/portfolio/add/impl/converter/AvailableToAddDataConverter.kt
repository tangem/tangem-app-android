package com.tangem.features.markets.portfolio.add.impl.converter

import com.tangem.domain.markets.FilterAvailableNetworksForWalletUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.markets.portfolio.add.api.AvailableToAddAccount
import com.tangem.features.markets.portfolio.add.api.AvailableToAddData
import com.tangem.features.markets.portfolio.add.api.AvailableToAddWallet
import javax.inject.Inject

class AvailableToAddDataConverter @Inject constructor(
    private val filterAvailableNetworksForWalletUseCase: FilterAvailableNetworksForWalletUseCase,
) {

    suspend fun convert(
        balances: Map<UserWallet, PortfolioFetcher.PortfolioBalance>,
        availableNetworks: Set<TokenMarketInfo.Network>,
    ): AvailableToAddData {
        fun AccountStatus.getAvailableToAddAccount(): AvailableToAddAccount {
            val availableToAddNetworks = availableNetworks
                .filterTo(mutableSetOf()) { network -> !this.isAlreadyAddedNetworks(network) }
            return AvailableToAddAccount(
                account = this,
                availableNetworks = availableNetworks,
                availableToAddNetworks = availableToAddNetworks,
            )
        }

        fun UserWallet.getAvailableToAddWallet(balance: PortfolioFetcher.PortfolioBalance): AvailableToAddWallet {
            val filteredNetworks = this.filteredAvailableNetworks(availableNetworks)
            val accounts = balance.accountsBalance.accountStatuses
            val availableToAddAccounts: Map<AccountId, AvailableToAddAccount> = accounts
                .map { it.account.accountId to it.getAvailableToAddAccount() }
                .filter { (_, account) -> account.availableToAddNetworks.isNotEmpty() }
                .toMap()
            return AvailableToAddWallet(
                userWallet = this,
                accounts = accounts,
                availableNetworks = filteredNetworks,
                availableToAddAccounts = availableToAddAccounts,
            )
        }

        val availableToAddWallets: Map<UserWalletId, AvailableToAddWallet> = balances
            .map { (wallet, balance) ->
                val availableToAddWallet = wallet.getAvailableToAddWallet(balance)
                wallet.walletId to availableToAddWallet
            }
            .filter { (_, wallet) -> wallet.availableToAddAccounts.isNotEmpty() }
            .toMap()

        return AvailableToAddData(
            availableToAddWallets = availableToAddWallets,
        )
    }

    private fun UserWallet.filteredAvailableNetworks(networks: Set<TokenMarketInfo.Network>) =
        filterAvailableNetworksForWalletUseCase(
            userWalletId = this.walletId,
            networks = networks,
        )

    private fun AccountStatus.isAlreadyAddedNetworks(network: TokenMarketInfo.Network): Boolean = when (this) {
        is AccountStatus.CryptoPortfolio -> this.tokenList.flattenCurrencies()
            .any { status -> network.networkId == status.currency.network.backendId }
    }
}