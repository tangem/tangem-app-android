package com.tangem.features.foryou.impl.tokensummary.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.markets.FetchCoinIndicatorsUseCase
import com.tangem.domain.markets.GetCoinIndicatorsUpdatesUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.analytics.ForYouAnalyticsEvent
import com.tangem.features.foryou.impl.analytics.toAnalyticsTokenAndNetwork
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.model.ForYouPeriod
import com.tangem.features.foryou.impl.tokensummary.entity.*
import com.tangem.features.foryou.impl.tokensummary.model.converter.AddToPortfolioTargetConverter
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
@Suppress("LongParameterList")
internal class TokenSummaryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val appRouter: AppRouter,
    private val messageSender: UiMessageSender,
    private val fetchCoinIndicatorsUseCase: FetchCoinIndicatorsUseCase,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
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

    val bottomSheetNavigation: SlotNavigation<TokenSummaryBottomSheetConfig> = SlotNavigation()

    private val swapHoldingsDelegate = swapHoldingsDelegateFactory.create(
        modelScope = modelScope,
        token = params.token,
    )

    private val addToPortfolioTarget = AddToPortfolioTargetConverter().convert(params.token)

    private val bottomButtonUMConverter = BottomButtonUMConverter(
        isAddToPortfolioAvailable = addToPortfolioTarget != null,
        onAddToPortfolioClick = ::openAddToPortfolio,
        onAddFundsClick = ::openManageFunds,
        onSwapClick = ::openSwap,
    )

    /**
     * Built on the first add-to-portfolio click and reused afterwards: the summary token never changes, so the manager
     * only has to be told about it once. `null` for a token the flow has nothing to add.
     */
    val addToPortfolioManager: AddToPortfolioManager? by lazy {
        addToPortfolioTarget?.let(::createAddToPortfolioManager)
    }

    val swapChooserCallbacks = object : SwapTokenChooserComponent.ModelCallbacks {
        override fun onHoldingSelected(holding: SwapHolding) = onSwapHoldingChosen(holding)

        override fun onDismiss() = bottomSheetNavigation.dismiss()
    }

    val swapHoldings: StateFlow<List<SwapHolding>>
        field = MutableStateFlow<List<SwapHolding>>(value = emptyList())

    val uiState: StateFlow<TokenSummaryUm>
        field = MutableStateFlow<TokenSummaryUm>(buildInitialUiState())

    init {
        val (token, network) = params.token.toAnalyticsTokenAndNetwork()
        analyticsEventHandler.send(ForYouAnalyticsEvent.TokenSummary(token = token, blockchain = network))

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
            .onEach { state ->
                uiState.update { it.copy(bottomButton = bottomButtonUMConverter.convert(state)) }
                swapHoldings.value = (state as? SwapHoldingsState.Resolved)?.holdings.orEmpty()
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

        val (token, network) = params.token.toAnalyticsTokenAndNetwork()
        analyticsEventHandler.send(
            ForYouAnalyticsEvent.TokenSummaryInterval(
                token = token,
                blockchain = network,
                period = ForYouPeriod.fromId(tangemSegmentUM.id).analyticsValue,
            ),
        )

        selectedTokenPeriodId.value = tangemSegmentUM.id
    }

    /**
     * Both this and [openAddToPortfolio] sit behind the same "Add funds" label
     * (see `BottomButtonUMConverter`), so they report the same event — the user sees one button.
     */
    private fun openManageFunds() {
        val rawCurrencyId = params.token.rawCurrencyId ?: return

        val (token, network) = params.token.toAnalyticsTokenAndNetwork()
        analyticsEventHandler.send(ForYouAnalyticsEvent.AddFunds(token = token, blockchain = network))
        bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.ManageFunds(rawCurrencyId))
    }

    private fun openAddToPortfolio() {
        if (addToPortfolioManager == null) return

        val (token, network) = params.token.toAnalyticsTokenAndNetwork()
        analyticsEventHandler.send(ForYouAnalyticsEvent.AddFunds(token = token, blockchain = network))
        bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.AddToPortfolio)
    }

    private fun createAddToPortfolioManager(target: AddToPortfolioTarget): AddToPortfolioManager {
        val manager = addToPortfolioManagerFactory.create(
            scope = modelScope,
            // The user opened the summary of this very token, so the token actions step has nothing left to offer.
            settings = AddToPortfolioManager.Settings(shouldSkipTokenActionsScreen = true),
            analyticsParams = AddToPortfolioManager.AnalyticsParams(
                source = AnalyticsParam.ScreensSources.Markets.value,
            ),
        ).apply {
            updateLaunchMode(AddToPortfolioManager.LaunchMode.ViaUserPortfolio)
            setTokenParams(target.token)
            setTokenNetworks(target.networks)
        }

        manager.onDismiss.receiveAsFlow()
            .onEach { bottomSheetNavigation.dismiss() }
            .launchIn(modelScope)

        merge(manager.onSuccessAdded.receiveAsFlow(), manager.onAddedTokenClick.receiveAsFlow())
            .onEach { result -> onTokenAdded(result) }
            .launchIn(modelScope)

        return manager
    }

    private fun onTokenAdded(result: AddToPortfolioManager.Result) {
        bottomSheetNavigation.dismiss()
        appRouter.push(
            AppRoute.CurrencyDetails(
                userWalletId = result.wallet.walletId,
                currency = result.addedCurrency.currency,
            ),
        )
    }

    private fun openSwap(holdings: List<SwapHolding>) {
        val (token, network) = params.token.toAnalyticsTokenAndNetwork()
        analyticsEventHandler.send(ForYouAnalyticsEvent.GoToSwap(token = token, blockchain = network))

        val onlyHolding = holdings.singleOrNull()

        when {
            onlyHolding != null -> onSwapHoldingChosen(onlyHolding)
            holdings.isNotEmpty() -> bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.SwapChooser)
        }
    }

    private fun onSwapHoldingChosen(holding: SwapHolding) {
        if (!holding.isSwapAvailable) {
            messageSender.send(DialogMessage(message = holding.unavailabilityReason.getUnavailabilityReasonText()))
            return
        }

        bottomSheetNavigation.dismiss()
        navigateToSwap(
            userWalletId = holding.entry.wallet.walletId,
            currency = holding.entry.currencyStatus.currency,
        )
    }

    private fun navigateToSwap(userWalletId: UserWalletId, currency: CryptoCurrency) {
        appRouter.push(
            AppRoute.Swap(
                userWalletId = userWalletId,
                fromCryptoCurrency = currency,
                screenSource = AnalyticsParam.ScreensSources.ForYou.value,
            ),
        )
    }

    private fun onInfoClick(indicator: TokenIndicatorUM.Loaded) {
        analyticsEventHandler.send(ForYouAnalyticsEvent.IndicatorInfo(info = indicator.indicatorType.analyticsValue))
        bottomSheetNavigation.activate(
            TokenSummaryBottomSheetConfig.Info(indicatorType = indicator.indicatorType, title = indicator.title),
        )
    }
}