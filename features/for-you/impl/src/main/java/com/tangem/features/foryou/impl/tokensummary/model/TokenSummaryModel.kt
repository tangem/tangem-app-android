package com.tangem.features.foryou.impl.tokensummary.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.producer.SingleAccountStatusProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier
import com.tangem.domain.markets.FetchCoinIndicatorsUseCase
import com.tangem.domain.markets.GetCoinIndicatorsUpdatesUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.model.ForYouPeriod
import com.tangem.features.foryou.impl.tokensummary.entity.*
import com.tangem.features.foryou.impl.tokensummary.model.transformer.SetTokenSentimentTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
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
    private val portfolioFetcherFactory: PortfolioFetcher.Factory,
    private val singleAccountStatusSupplier: SingleAccountStatusSupplier,
    private val fetchCoinIndicatorsUseCase: FetchCoinIndicatorsUseCase,
    private val getCoinIndicatorsUpdatesUseCase: GetCoinIndicatorsUpdatesUseCase,
    val portfolioSelectorController: PortfolioSelectorController,
) : Model() {

    private val params = paramsContainer.require<TokenSummaryComponent.Params>()

    private val tokenSymbol: String = when (val token = params.token) {
        is TokenSummaryComponent.Token.Portfolio -> token.cryptoCurrency.symbol
        is TokenSummaryComponent.Token.Market -> token.symbol
    }

    private val iconConverter = CryptoCurrencyToIconStateConverter()
    private val swapNavigationJob = JobHolder()

    private val selectedTokenPeriodId = MutableStateFlow(value = params.selectedTokenPeriodId)

    /** Drives the single bottom-sheet slot hosted by the component (portfolio selector / info). */
    val bottomSheetNavigation: SlotNavigation<TokenSummaryBottomSheetConfig> = SlotNavigation()

    /** Feeds the portfolio selector with the wallet's accounts. */
    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.Wallet(params.userWalletId),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { bottomSheetNavigation.dismiss() }
        override val onBack: () -> Unit = { bottomSheetNavigation.dismiss() }
    }

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
            onSwapClick = ::onSwapClicked,
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

    private fun onSwapClicked() {
        modelScope.launch {
            val account = if (portfolioSelectorController.isAccountModeSync()) {
                portfolioSelectorController.selectAccount(null)
                bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.PortfolioSelector)

                val (_, selectedAccount) = portfolioSelectorController
                    .selectedAccountWithData(portfolioFetcher)
                    .filterNotNull()
                    .first()

                bottomSheetNavigation.dismiss()
                selectedAccount
            } else {
                singleAccountStatusSupplier(
                    SingleAccountStatusProducer.Params(
                        accountId = AccountId.forMainCryptoPortfolio(params.userWalletId),
                    ),
                )
                    .filterIsInstance<AccountStatus.CryptoPortfolio>()
                    .first()
            }

            val currency = account.flattenCurrencies()
                .map(CryptoCurrencyStatus::currency)
                .firstOrNull(::matchesSummaryToken)

            navigateToSwap(currency)
        }.saveIn(swapNavigationJob)
    }

    private fun matchesSummaryToken(currency: CryptoCurrency): Boolean = when (val token = params.token) {
        is TokenSummaryComponent.Token.Portfolio -> {
            val summaryCurrency = token.cryptoCurrency
            currency.id.rawCurrencyId == summaryCurrency.id.rawCurrencyId && currency.network == summaryCurrency.network
        }
        is TokenSummaryComponent.Token.Market -> currency.id.rawCurrencyId == token.cryptoCurrencyRawId
    }

    private fun navigateToSwap(currency: CryptoCurrency?) {
        appRouter.push(
            AppRoute.Swap(
                userWalletId = params.userWalletId,
                fromCryptoCurrency = currency,
                screenSource = "screen source", // TODO
            ),
        )
    }

    private fun onInfoClick(indicatorType: IndicatorType) {
        bottomSheetNavigation.activate(TokenSummaryBottomSheetConfig.Info(indicatorType))
    }
}