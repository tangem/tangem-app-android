package com.tangem.features.feed.components.market.details.portfolio.add.impl.converter

import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.markets.FilterAvailableNetworksForWalletUseCase
import com.tangem.domain.markets.GetTokenMarketCryptoCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.feed.components.market.details.portfolio.add.api.AvailableToAddAccount
import com.tangem.features.feed.components.market.details.portfolio.add.api.AvailableToAddData
import com.tangem.features.feed.components.market.details.portfolio.add.api.AvailableToAddWallet
import javax.inject.Inject

internal class AvailableToAddDataConverter @Inject constructor(
    private val filterAvailableNetworksForWalletUseCase: FilterAvailableNetworksForWalletUseCase,
    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
) {

    suspend fun convert(
        balances: Map<UserWalletId, PortfolioFetcher.PortfolioBalance>,
        availableNetworks: Set<TokenMarketInfo.Network>,
        marketParams: TokenMarketParams,
    ): AvailableToAddData {
        suspend fun AccountStatus.getAvailableToAddAccount(wallet: UserWallet): AvailableToAddAccount? {
            val currencies = availableNetworks
                .mapNotNull { network ->
                    createCryptoCurrency(
                        userWallet = wallet,
                        network = network,
                        marketParams = marketParams,
                        account = this.account,
                    )
                }

            if (currencies.isEmpty()) return null

            val addedNetworks = getAccountCurrencyStatusUseCase.invokeSync(wallet.walletId, currencies)
                .fold(
                    ifEmpty = { emptySet() },
                    ifSome = { map ->
                        map.values.flatMapTo(hashSetOf()) { statuses ->
                            statuses.map { it.currency.network }
                        }
                    },
                )

            return AvailableToAddAccount(
                account = this,
                availableNetworks = availableNetworks,
                addedNetworks = addedNetworks,
            )
        }

        suspend fun getAvailableToAddWallet(
            entry: Map.Entry<UserWalletId, PortfolioFetcher.PortfolioBalance>,
        ): AvailableToAddWallet {
            val (_, balance) = entry
            val wallet = balance.userWallet
            val filteredNetworks = wallet.filteredAvailableNetworks(availableNetworks)
            val accounts = balance.accountsBalance.accountStatuses
            val availableToAddAccounts: Map<AccountId, AvailableToAddAccount> = accounts
                .mapNotNull { accountStatus ->
                    val availableToAddAccount = accountStatus.getAvailableToAddAccount(wallet) ?: return@mapNotNull null
                    accountStatus.account.accountId to availableToAddAccount
                }
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
            .map { entry ->
                val (walletId, _) = entry
                val availableToAddWallet = getAvailableToAddWallet(entry)
                walletId to availableToAddWallet
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
        account: Account,
    ): CryptoCurrency? {
        val derivationIndex = when (account) {
            is Account.CryptoPortfolio -> account.derivationIndex
        }
        return getTokenMarketCryptoCurrency(
            userWalletId = userWallet.walletId,
            tokenMarketParams = marketParams,
            network = network,
            accountIndex = derivationIndex,
        )
    }
}