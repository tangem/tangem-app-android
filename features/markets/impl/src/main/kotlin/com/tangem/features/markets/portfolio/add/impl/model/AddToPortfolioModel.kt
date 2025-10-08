package com.tangem.features.markets.portfolio.add.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.markets.FilterAvailableNetworksForWalletUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.markets.portfolio.add.api.AddToPortfolioComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ModelScoped
internal class AddToPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val filterAvailableNetworksForWalletUseCase: FilterAvailableNetworksForWalletUseCase,
    val portfolioSelectorController: PortfolioSelectorController,
) : Model() {

    private val params = paramsContainer.require<AddToPortfolioComponent.Params>()

    val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(onlyMultiCurrency = true),
        scope = modelScope,
    )
    val stackNavigation = StackNavigation<AddToPortfolioRoutes>()

    init {
        combine(
            flow = portfolioFetcher.data.map { it.balances }.distinctUntilChanged(),
            flow2 = params.manager.availableNetworks.map { it.toSet() }.distinctUntilChanged(),
            flow3 = portfolioSelectorController.isAccountMode,
        ) { balances, availableNetworks, isAccountMode ->
            val availableToAddWallets: Map<UserWalletId, AvailableToAddWallet> = balances.map { (wallet, balance) ->
                val filteredNetworks = wallet.filteredAvailableNetworks(availableNetworks)
                val availableToAdd = AvailableToAddWallet(
                    userWallet = wallet,
                    accounts = balance.accountsBalance.accountStatuses,
                    availableNetworks = filteredNetworks,
                )
                wallet.walletId to availableToAdd
            }.filter { (_, wallet) -> wallet.availableToAddAccounts.isNotEmpty() }.toMap()

            portfolioSelectorController.isEnabled = isEnabled@{ userWallet, accountStatus ->
                val availableWallet = availableToAddWallets[userWallet.walletId] ?: return@isEnabled false
                val availableAccount =
                    availableWallet.availableToAddAccounts[accountStatus.account.accountId] ?: return@isEnabled false
                return@isEnabled true
            }
            FeatureRequireData(
                availableToAddWallets = availableToAddWallets,
            )
        }
            .launchIn(modelScope)
    }

    private fun UserWallet.filteredAvailableNetworks(networks: Set<TokenMarketInfo.Network>) =
        filterAvailableNetworksForWalletUseCase(
            userWalletId = this.walletId,
            networks = networks,
        )

    data class FeatureRequireData(
        val availableToAddWallets: Map<UserWalletId, AvailableToAddWallet>,
    ) {
        val isSinglePortfolio: Boolean
            get() = availableToAddWallets.size <= 1 && availableToAddWallets.values.first().accounts.size <= 1

        val singlePortfolio: SelectedPortfolio
            get() = availableToAddWallets.values.first().let {
                SelectedPortfolio(
                    userWallet = it.userWallet,
                    account = it.accounts.first(),
                )
            }
    }

    data class AvailableToAddWallet(
        val userWallet: UserWallet,
        val accounts: Set<AccountStatus>,
        val availableNetworks: Set<TokenMarketInfo.Network>,
    ) {

        val isSingleAvailableNetworks: Boolean get() = availableNetworks.size <= 1
        val singleAvailableNetworks: TokenMarketInfo.Network get() = availableNetworks.first()

        val availableToAddAccounts: Map<AccountId, AvailableToAddAccount> by lazy {
            accounts.map { it.account.accountId to AvailableToAddAccount(it, availableNetworks) }
                .filter { (_, account) -> account.availableToAddNetworks.isNotEmpty() }.toMap()
        }
    }

    data class AvailableToAddAccount(
        val account: AccountStatus,
        val availableNetworks: Set<TokenMarketInfo.Network>,
    ) {
        val availableToAddNetworks: Set<TokenMarketInfo.Network> by lazy {
            availableNetworks.filterTo(mutableSetOf()) { network -> !account.isAlreadyAddedNetworks(network) }
        }
    }

    @Serializable
    data class SelectedPortfolio(
        val userWallet: UserWallet,
        val account: AccountStatus,
    )

    data class ToAddRequireData(
        val selectedPortfolio: SelectedPortfolio?,
        val selectedNetwork: TokenMarketInfo.Network?,
    ) {

        val cryptoCurrencyStatus: CryptoCurrencyStatus? by lazy {
            selectedPortfolio ?: return@lazy null
            selectedNetwork ?: return@lazy null
            when (val account = selectedPortfolio.account) {
                is AccountStatus.CryptoPortfolio -> account.tokenList.flattenCurrencies()
                    .first { status -> selectedNetwork.networkId == status.currency.network.backendId }
            }
        }
    }
}

private fun AccountStatus.isAlreadyAddedNetworks(network: TokenMarketInfo.Network): Boolean = when (this) {
    is AccountStatus.CryptoPortfolio -> this.tokenList.flattenCurrencies()
        .any { status -> network.networkId == status.currency.network.backendId }
}