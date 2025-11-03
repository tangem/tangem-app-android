package com.tangem.features.markets.portfolio.add.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.SaveCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.ColdWalletAndHasMissedDerivationsUseCase
import com.tangem.features.markets.portfolio.add.api.SelectedNetwork
import com.tangem.features.markets.portfolio.add.api.SelectedPortfolio
import com.tangem.features.markets.portfolio.add.impl.AddTokenComponent
import com.tangem.features.markets.portfolio.add.impl.model.AddTokenUiBuilder.Companion.toggleProgress
import com.tangem.features.markets.portfolio.add.impl.ui.state.AddTokenUM
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class AddTokenModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val uiBuilder: AddTokenUiBuilder,
    private val coldWalletAndHasMissedDerivationsUseCase: ColdWalletAndHasMissedDerivationsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val saveCryptoCurrenciesUseCase: SaveCryptoCurrenciesUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
) : Model() {

    private val params = paramsContainer.require<AddTokenComponent.Params>()
    private val analyticsEventBuilder = params.eventBuilder
    private val addTokenJob = JobHolder()

    val uiState: StateFlow<AddTokenUM?>
        field = MutableStateFlow(value = null)

    init {
        combine(
            flow = params.selectedNetwork.distinctUntilChanged(),
            flow2 = params.selectedPortfolio.distinctUntilChanged(),
            transform = { selectedNetwork, selectedPortfolio ->
                addTokenJob.cancel()
                val isTangemIconVisible = needColdWalletInteraction(selectedNetwork, selectedPortfolio)
                uiBuilder.updateContent(
                    selectedPortfolio = selectedPortfolio,
                    selectedNetwork = selectedNetwork,
                    isTangemIconVisible = isTangemIconVisible,
                    onConfirmClick = { onAddClick(selectedNetwork, selectedPortfolio).saveIn(addTokenJob) },
                )
            },
        )
            .onEach { newUI -> uiState.value = newUI }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun onAddClick(selectedNetwork: SelectedNetwork, selectedPortfolio: SelectedPortfolio) =
        modelScope.launch(dispatchers.default) {
            val um = uiState.value ?: return@launch
            uiState.value = um.toggleProgress(true)
            val blockchainNames = listOf(selectedNetwork.selectedNetwork)
                .mapNotNull { BlockchainUtils.getNetworkInfo(it.networkId)?.name }
            analyticsEventHandler.send(analyticsEventBuilder.addToPortfolioContinue(blockchainNames))
            val cryptoCurrency = selectedNetwork.cryptoCurrency
            val accountId = selectedPortfolio.account.account.account.accountId
            saveCryptoCurrenciesUseCase(
                accountId = accountId,
                add = listOf(cryptoCurrency),
                remove = listOf(),
            )
            val status = getAccountCurrencyStatusUseCase.invokeSync(
                userWalletId = accountId.userWalletId,
                currencyId = cryptoCurrency.id,
                network = cryptoCurrency.network,
            ).getOrNull() ?: return@launch
            params.callbacks.onTokenAdded(status.status)
            uiState.value = um.toggleProgress(false)
        }

    private suspend fun needColdWalletInteraction(
        selectedNetwork: SelectedNetwork,
        selectedPortfolio: SelectedPortfolio,
    ): Boolean = coldWalletAndHasMissedDerivationsUseCase.invoke(
        userWalletId = selectedPortfolio.userWallet.walletId,
        networksWithDerivationPath = mapOf(selectedNetwork.selectedNetwork.networkId to null),
    )
}