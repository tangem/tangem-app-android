package com.tangem.features.foryou.impl.tokensummary.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.markets.FetchCoinIndicatorsUseCase
import com.tangem.domain.markets.GetCoinIndicatorsUpdatesUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.model.ForYouPeriod
import com.tangem.features.foryou.impl.tokensummary.entity.*
import com.tangem.features.foryou.impl.tokensummary.model.converter.BottomButtonUMConverter
import com.tangem.features.foryou.impl.tokensummary.model.transformer.SetTokenSentimentTransformer
import com.tangem.features.foryou.impl.tokensummary.swapchooser.SwapTokenChooserComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TokenSummaryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val appRouter: AppRouter,
    private val fetchCoinIndicatorsUseCase: FetchCoinIndicatorsUseCase,
    getCoinIndicatorsUpdatesUseCase: GetCoinIndicatorsUpdatesUseCase,
    swapHoldingsDelegateFactory: SwapHoldingsDelegate.Factory,
) : Model() {

    private val params = paramsContainer.require<TokenSummaryComponent.Params>()

    private val tokenSymbol: String = when (val token = params.token) {
        is TokenSummaryComponent.Token.Portfolio -> token.cryptoCurrency.symbol
        is TokenSummaryComponent.Token.Market -> token.symbol
    }

    private val iconConverter = CryptoCurrencyToIconStateConverter()
    private val selectedTokenPeriodId = MutableStateFlow(value = params.selectedTokenPeriodId)

    private val swapHoldingsDelegate = swapHoldingsDelegateFactory.create(
        modelScope = modelScope,
        token = params.token,
    )

    private val bottomButtonUMConverter = BottomButtonUMConverter(
        onAddFundsClick = ::openManageFunds,
        onSwapClick = ::openSwap,
    )

    val bottomSheetNavigation: SlotNavigation<TokenSummaryBottomSheetConfig> = SlotNavigation()

    val swapChooserCallbacks = object : SwapTokenChooserComponent.ModelCallbacks {
        override fun onTokenSelected(userWalletId: UserWalletId, status: CryptoCurrencyStatus) =
            onSwapTokenChosen(userWalletId, status)

        override fun onDismiss() = bottomSheetNavigation.dismiss()
    }

    val swapEntries: StateFlow<List<TokenSelectorEntry>>
        field = MutableStateFlow<List<TokenSelectorEntry>>(value = emptyList())

    val uiState: StateFlow<TokenSummaryUm>
        field = MutableStateFlow<TokenSummaryUm>(buildInitialUiState())

    init {
        modelScope.launch {
            fetchCoinIndicatorsUseCase(symbols = listOf(tokenSymbol))
        }

        combine(
            getCoinIndicatorsUpdatesUseCase().map { it[tokenSymbol.uppercase()] }.distinctUntilChanged(),
            selectedTokenPeriodId,
        ) { coinIndicators, periodId ->
            uiState.update(
                SetTokenSentimentTransformer(coinIndicators, periodId),
            )
        }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)

        swapHoldingsDelegate.state
            .onEach { holdings ->
                uiState.update {
                    it.copy(bottomButton = bottomButtonUMConverter.convert(holdings))
                }
                swapEntries.value = (holdings as? SwapHoldingsState.Available)?.entries.orEmpty()
            }
            .launchIn(modelScope)
    }

    private fun buildInitialUiState(): TokenSummaryUm {
        val periodItems = ForYouPeriod.entries
            .map { period -> TangemSegmentUM(id = period.id, title = period.title) }
            .toPersistentList()

        return TokenSummaryUm(
            header = buildHeader(),
            periodPicker = PeriodPickerUM.Content(
                TangemSegmentedPickerUM(
                    items = periodItems,
                    initialSelectedItem = periodItems.firstOrNull { it.id == params.selectedTokenPeriodId },
                    isFixed = true,
                    isAltSurface = true,
                ),
            ),
            tokenSentiment = TokenSentimentUM.Loading,
            aiInsight = AiInsightUM.Hide,
            bottomButton = BottomButtonUM.Loading,
            onPeriodClick = ::onPeriodClick,
            onInfoClick = ::onInfoClick,
            onCloseClick = params.callbacks::onDismiss,
        )
    }

    private fun buildHeader(): TokenSummaryHeaderUM = when (val token = params.token) {
        is TokenSummaryComponent.Token.Portfolio -> {
            val currency = token.cryptoCurrency
            TokenSummaryHeaderUM(
                tangemIconUM = TangemIconUM.Currency(iconConverter.convert(currency)),
                title = stringReference(currency.name.ifBlank { currency.symbol }),
                subtitle = stringReference(currency.network.name),
            )
        }
        is TokenSummaryComponent.Token.Market -> TokenSummaryHeaderUM(
            tangemIconUM = TangemIconUM.Url(url = token.tangemIconUrl, fallbackRes = R.drawable.ic_custom_token_44),
            title = stringReference(token.title),
            subtitle = null,
        )
    }

    private fun onPeriodClick(tangemSegmentUM: TangemSegmentUM) {
        if (tangemSegmentUM.id == selectedTokenPeriodId.value) return

        selectedTokenPeriodId.value = tangemSegmentUM.id
    }

    private fun openManageFunds() {
        val rawCurrencyId = params.token.rawCurrencyId ?: return

        bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.ManageFunds(rawCurrencyId))
    }

    private fun openSwap(entries: List<TokenSelectorEntry>) {
        val onlyEntry = entries.singleOrNull()

        when {
            onlyEntry != null -> navigateToSwap(onlyEntry.wallet.walletId, onlyEntry.currencyStatus.currency)
            entries.isNotEmpty() -> bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.SwapChooser)
        }
    }

    private fun onSwapTokenChosen(userWalletId: UserWalletId, status: CryptoCurrencyStatus) {
        bottomSheetNavigation.dismiss()
        navigateToSwap(userWalletId, status.currency)
    }

    private fun navigateToSwap(userWalletId: UserWalletId, currency: CryptoCurrency) {
        appRouter.push(
            AppRoute.Swap(
                userWalletId = userWalletId,
                fromCryptoCurrency = currency,
                // TODO ask about right value
                screenSource = "Token summary", // AnalyticsParam.ScreensSources.TokenSummary
            ),
        )
    }

    private fun onInfoClick(indicatorType: IndicatorType) {
        bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.Info(indicatorType))
    }
}