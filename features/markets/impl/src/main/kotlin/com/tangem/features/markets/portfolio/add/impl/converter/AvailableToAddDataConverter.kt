package com.tangem.features.markets.portfolio.add.impl.converter

import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.markets.FilterAvailableNetworksForWalletUseCase
import com.tangem.domain.markets.GetTokenMarketCryptoCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.markets.portfolio.add.api.AvailableToAddAccount
import com.tangem.features.markets.portfolio.add.api.AvailableToAddData
import com.tangem.features.markets.portfolio.add.api.AvailableToAddWallet
import javax.inject.Inject

internal class AvailableToAddDataConverter @Inject constructor(
    private val filterAvailableNetworksForWalletUseCase: FilterAvailableNetworksForWalletUseCase,
    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
) {

    suspend fun convert(
        balances: Map<UserWallet, PortfolioFetcher.PortfolioBalance>,
        availableNetworks: Set<TokenMarketInfo.Network>,
        marketParams: TokenMarketParams,
    ): AvailableToAddData {
        suspend fun AccountStatus.getAvailableToAddAccount(wallet: UserWallet): AvailableToAddAccount {
            val addedNetworks = availableNetworks
                .mapNotNull { createCryptoCurrency(wallet, it, marketParams) }
                .mapNotNull { getAccountCurrencyStatusUseCase.invokeSync(wallet.walletId, it) }
                .mapNotNull { it.getOrNull()?.status?.currency?.network }
                .toSet()
            return AvailableToAddAccount(
                account = this,
                availableNetworks = availableNetworks,
                addedNetworks = addedNetworks,
            )
        }

        suspend fun getAvailableToAddWallet(
            entry: Map.Entry<UserWallet, PortfolioFetcher.PortfolioBalance>,
        ): AvailableToAddWallet {
            val (wallet, balance) = entry
            val filteredNetworks = wallet.filteredAvailableNetworks(availableNetworks)
            val accounts = balance.accountsBalance.accountStatuses
            val availableToAddAccounts: Map<AccountId, AvailableToAddAccount> = accounts
                .map { it.account.accountId to it.getAvailableToAddAccount(wallet) }
                .filter { (_, account) -> account.availableToAddNetworks.isNotEmpty() }
                .toMap()
            return AvailableToAddWallet(
                userWallet = wallet,
                accounts = accounts,
                availableNetworks = filteredNetworks,
                availableToAddAccounts = availableToAddAccounts,
            )
        }

        val availableToAddWallets: Map<UserWalletId, AvailableToAddWallet> = balances
            .map {
                val (wallet, balance) = it
                val availableToAddWallet = getAvailableToAddWallet(it)
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

    private suspend fun createCryptoCurrency(
        userWallet: UserWallet,
        network: TokenMarketInfo.Network,
        marketParams: TokenMarketParams,
    ): CryptoCurrency? = getTokenMarketCryptoCurrency(
        userWalletId = userWallet.walletId,
        tokenMarketParams = marketParams,
        network = network,
    )
}