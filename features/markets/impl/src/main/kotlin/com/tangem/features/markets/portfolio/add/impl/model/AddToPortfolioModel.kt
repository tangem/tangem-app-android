package com.tangem.features.markets.portfolio.add.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.markets.portfolio.add.api.AddToPortfolioComponent
import com.tangem.features.markets.portfolio.add.impl.converter.AvailableToAddDataConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ModelScoped
internal class AddToPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val availableToAddDataConverter: AvailableToAddDataConverter,
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
            val availableToAdd = availableToAddDataConverter.convert(balances, availableNetworks)

            portfolioSelectorController.isEnabled.value = isEnabled@{ userWallet, accountStatus ->
                val availableWallet = availableToAdd.availableToAddWallets[userWallet.walletId]
                    ?: return@isEnabled false
                availableWallet.availableToAddAccounts[accountStatus.account.accountId] ?: return@isEnabled false
                return@isEnabled true
            }
            availableToAdd
        }
            .launchIn(modelScope)
    }
}