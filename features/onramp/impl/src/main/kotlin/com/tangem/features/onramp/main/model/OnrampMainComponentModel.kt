package com.tangem.features.onramp.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.deeplink.resolveMarketingDeeplink
import com.tangem.common.routing.deeplink.toContextualRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.marketing.api.LinkedBannerRequest
import com.tangem.features.marketing.api.MarketingBannerRequest
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.*
import com.tangem.features.onramp.main.entity.factory.OnrampAmountButtonUMStateFactory
import com.tangem.features.onramp.main.entity.factory.OnrampAmountStateFactory
import com.tangem.features.onramp.main.entity.factory.OnrampOffersStateFactory
import com.tangem.features.onramp.main.entity.factory.OnrampStateFactory
import com.tangem.features.onramp.utils.sendOnrampErrorEvent
import com.tangem.features.onramp.utils.sendProviderCalculatedEvent
import com.tangem.features.onramp.utils.showDemoModeWarningIfNeeded
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.isNullOrZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
internal class OnrampMainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    private val clearOnrampCacheUseCase: ClearOnrampCacheUseCase,
    private val fetchQuotesUseCase: OnrampFetchQuotesUseCase,
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase,
    private val fetchPairsUseCase: OnrampFetchPairsUseCase,
    private val rampStateManager: RampStateManager,
    private val amountInputManager: InputManager,
    private val getOnrampOffersUseCase: GetOnrampOffersUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val messageSender: UiMessageSender,
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase,
    paramsContainer: ParamsContainer,
    getWalletsUseCase: GetWalletsUseCase,
) : Model(), OnrampIntents {

    val params = paramsContainer.require<OnrampMainComponent.Params>()

    private val onrampAmountButtonUMStateFactory: OnrampAmountButtonUMStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampAmountButtonUMStateFactory()
    }

    @Suppress("PropertyUsedBeforeDeclaration")
    private val stateFactory: OnrampStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampStateFactory(
            currentStateProvider = Provider { state.value },
            cryptoCurrency = params.cryptoCurrency,
            onrampIntents = this,
            onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
        )
    }

    val state: StateFlow<OnrampMainComponentUM>
        field = MutableStateFlow<OnrampMainComponentUM>(
            value = stateFactory.getInitialState(
                currency = params.cryptoCurrency.name,
                onClose = ::onCloseClick,
                openSettings = ::openSettings,
            ),
        )

    /**
     * Expected received crypto amount, taken from the first [OnrampQuote.Data] quote's [OnrampQuote.Data.toAmount].
     * Used to derive [amountUsd][MarketingBannerRequest.amountUsd] for the marketing banner request flows below,
     * since the campaign min/max amount gating is expressed in USD while the user only enters a fiat amount here.
     *
     * Seeded with an initial `null` via [onStart] so the downstream [combine] can emit immediately on cold start
     * (before any quote is available, e.g. when the user has not entered an amount yet). Without this seed the
     * underlying quotes flow stays silent until a quote is stored, which would keep the banner requests from
     * emitting at all.
     */
    private val expectedCryptoAmount: Flow<BigDecimal?> = getOnrampQuotesUseCase.invoke()
        .map { either ->
            either.getOrNull()
                ?.filterIsInstance<OnrampQuote.Data>()
                ?.firstOrNull()
                ?.toAmount?.value
        }
        .distinctUntilChanged()
        .onStart { emit(null) }

    /**
     * Request flow for the standalone marketing banner.
     * [fromFiat] is the fiat currency code the user is paying in (only available once the screen is in Content state).
     * [toNetwork] is the backend network id of the target crypto currency.
     * [toContractAddress] is the contract address of the target token (empty string for coins).
     * [amountUsd] is derived from the expected received crypto amount (see [expectedCryptoAmount]) converted to USD.
     * On cold start (no quote yet, e.g. the user has not entered an amount) it is null, so the request emits
     * immediately with `amountUsd = null` and the domain shows the banner ungated; once a quote arrives the request
     * re-emits with the real USD amount so the min/max filter applies. It is also null if the target currency has no
     * USD rate, again skipping the amount filter.
     */
    val marketingRequest: Flow<MarketingBannerRequest?> = combine(state, expectedCryptoAmount) { s, crypto ->
        val contentState = s as? OnrampMainComponentUM.Content ?: return@combine null
        MarketingBannerRequest(
            screen = MarketingScreen.Onramp(
                fromFiat = contentState.amountBlockState.currencyUM.code,
                toNetwork = params.cryptoCurrency.network.rawId,
                toContractAddress = (params.cryptoCurrency as? CryptoCurrency.Token)?.contractAddress.orEmpty(),
            ),
            amountUsd = computeAmountUsd(crypto),
        )
    }

    /**
     * Request flow for the LINKED_TO_PROVIDER marketing banner shown inline next to onramp provider offers.
     * Carries no provider id: provider matching is done per offer at render time (each offer row asks for its
     * banner via [MarketingBannerComponent.LinkedContent]), mirroring iOS.
     * [amountUsd] follows the same rules as in [marketingRequest]: null on cold start (banner shown ungated) or when
     * the target currency has no USD rate, and the real USD amount once a quote is available.
     */
    val linkedMarketingRequest: Flow<LinkedBannerRequest?> = combine(state, expectedCryptoAmount) { s, crypto ->
        val contentState = s as? OnrampMainComponentUM.Content ?: return@combine null
        LinkedBannerRequest(
            screen = MarketingScreen.Onramp(
                fromFiat = contentState.amountBlockState.currencyUM.code,
                toNetwork = params.cryptoCurrency.network.rawId,
                toContractAddress = (params.cryptoCurrency as? CryptoCurrency.Token)?.contractAddress.orEmpty(),
            ),
            amountUsd = computeAmountUsd(crypto),
        )
    }

    private val amountStateFactory: OnrampAmountStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampAmountStateFactory(
            currentStateProvider = Provider { state.value },
            analyticsEventHandler = analyticsEventHandler,
            onrampIntents = this,
            onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
        )
    }

    private val onrampOffersStateFactory: OnrampOffersStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampOffersStateFactory(
            currentStateProvider = Provider { state.value },
            onrampIntents = this,
        )
    }

    private val quotesTaskScheduler = SingleTaskScheduler<Unit>()

    val bottomSheetNavigation: SlotNavigation<OnrampMainBottomSheetConfig> = SlotNavigation()
    val userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }

    init {
        modelScope.launch {
            clearOnrampCacheUseCase()
        }
        startLoadingQuotes()
        sendScreenOpenAnalytics()
        checkResidenceCountry()
        subscribeToAmountChanges()
        subscribeToCountryAndCurrencyUpdates()
        subscribeToQuotesUpdate()
        subscribeOnOffers()
    }

    override fun onDestroy() {
        modelScope.launch { clearOnrampCacheUseCase.invoke() }
        quotesTaskScheduler.cancelTask()
        super.onDestroy()
    }

    fun onMarketingBannerDeeplink(deeplink: String): Boolean {
        val route = resolveMarketingDeeplink(deeplink).toContextualRoute(
            userWalletId = params.userWalletId,
            currency = params.cryptoCurrency,
            screenSource = AnalyticsParam.ScreensSources.Buy,
        ) ?: return false
        router.push(route)
        return true
    }

    override fun onAmountValueChanged(value: String) {
        state.update { amountStateFactory.getOnAmountValueChange(value) }
        modelScope.launch { amountInputManager.update(value) }
    }

    override fun openSettings() {
        params.openSettings.invoke()
    }

    override fun openCurrenciesList() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.SelectCurrencyScreenOpened())
        bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.CurrenciesList)
    }

    override fun onBuyClick(
        quote: OnrampProviderWithQuote.Data,
        onrampOfferAdvantagesUM: OnrampOfferAdvantagesUM,
        categoryUM: OnrampOfferCategoryUM,
    ) {
        val currentContentState = state.value as? OnrampMainComponentUM.Content ?: return
        analyticsEventHandler.send(
            OnrampAnalyticsEvent.OnBuyClick(
                providerName = quote.provider.info.name,
                currency = currentContentState.amountBlockState.currencyUM.code,
                tokenSymbol = params.cryptoCurrency.symbol,
            ),
        )
        sendOfferClickEvent(
            quote = quote,
            onrampOfferAdvantagesUM = onrampOfferAdvantagesUM,
            categoryUM = categoryUM,
        )
        if (messageSender.showDemoModeWarningIfNeeded(userWallet, isDemoCardUseCase)) return
        params.openRedirectPage(quote)
    }

    override fun openProviders() {
        val currentContentState = state.value as? OnrampMainComponentUM.Content ?: return
        val amountCurrentCode = currentContentState.amountBlockState.currencyUM.code
        bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.AllOffers(amountCurrentCode))
    }

    override fun onRefresh() {
        state.update {
            stateFactory.getInitialState(
                currency = params.cryptoCurrency.name,
                onClose = router::pop,
                openSettings = ::openSettings,
            )
        }
        modelScope.launch {
            clearOnrampCacheUseCase.invoke()
            checkResidenceCountry()
            handleOnrampAvailable()
        }
    }

    fun onStop() {
        quotesTaskScheduler.cancelTask()
    }

    fun handleOnrampAvailable() {
        subscribeToCountryAndCurrencyUpdates()
        subscribeToQuotesUpdate()
    }

    private fun startLoadingQuotes() {
        quotesTaskScheduler.cancelTask()
        quotesTaskScheduler.scheduleTask(scope = modelScope, task = loadQuotesTask())
    }

    private fun loadQuotesTask(): PeriodicTask<Unit> {
        return PeriodicTask(
            delay = UPDATE_DELAY,
            task = {
                runSuspendCatching {
                    val amountBlockState = (state.value as? OnrampMainComponentUM.Content)?.amountBlockState
                        ?: return@runSuspendCatching

                    val fiatAmount = amountBlockState.amountFieldModel.fiatAmount
                    if (fiatAmount.value.isNullOrZero()) return@runSuspendCatching

                    fetchQuotesUseCase.invoke(
                        userWallet = userWallet,
                        amount = amountBlockState.amountFieldModel.fiatAmount,
                        cryptoCurrency = params.cryptoCurrency,
                    ).onLeft(::handleOnrampError)
                }
            },
            onSuccess = {},
            onError = {},
        )
    }

    private fun checkResidenceCountry() {
        modelScope.launch {
            checkOnrampAvailabilityUseCase(userWallet)
                .onRight(::handleOnrampAvailability)
                .onLeft(::handleOnrampError)
        }
    }

    private fun handleOnrampAvailability(availability: OnrampAvailability) {
        // "Buy not supported" notification has priority over the residency flow.
        if (state.value.buyNotSupportedMessage != null) return
        when (availability) {
            is OnrampAvailability.Available -> Unit
            is OnrampAvailability.ConfirmResidency,
            is OnrampAvailability.NotSupported,
            -> bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.ConfirmResidency(availability.country))
        }
    }

    private fun onCloseClick() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.CloseOnramp())
        router.pop()
    }

    private fun subscribeOnOffers() = modelScope.launch {
        getOnrampOffersUseCase
            .invoke()
            .collectLatest { maybeOffers ->
                maybeOffers.fold(
                    ifLeft = ::handleOnrampError,
                    ifRight = { offers ->
                        val currentState = state.value
                        if (currentState is OnrampMainComponentUM.Content) {
                            if (currentState.amountBlockState.amountFieldModel.fiatValue.isEmpty()) {
                                state.update {
                                    currentState.copy(offersBlockState = OnrampOffersBlockUM.Empty)
                                }
                                return@fold
                            }
                            state.update {
                                onrampOffersStateFactory.getOffersState(offers)
                            }
                        }
                    },
                )
            }
    }

    private fun subscribeToAmountChanges() = modelScope.launch {
        amountInputManager.query
            .filter(String::isNotEmpty)
            .collectLatest { _ ->
                startLoadingQuotes()
            }
    }

    private fun subscribeToCountryAndCurrencyUpdates() {
        getOnrampCountryUseCase.invoke()
            .onEach { maybeCountry ->
                maybeCountry.fold(
                    ifLeft = ::handleOnrampError,
                    ifRight = { country ->
                        if (country == null) return@onEach
                        // Resolve token-level buy support BEFORE emitting any Content state, so an
                        // unsupported token never briefly shows an enabled amount field — otherwise it
                        // would grab focus and flash the keyboard before being disabled.
                        if (isTokenNotSupportedForBuy()) {
                            showBuyNotSupported(country)
                        } else {
                            state.update { prevState -> getCountryUpdatedState(prevState, country) }
                            updatePairsAndQuotes()
                        }
                    },
                )
            }
            .launchIn(modelScope)
    }

    private fun getCountryUpdatedState(
        prevState: OnrampMainComponentUM,
        country: OnrampCountry,
    ): OnrampMainComponentUM = when (prevState) {
        is OnrampMainComponentUM.Content -> amountStateFactory.getUpdatedCurrencyState(country.defaultCurrency)
        is OnrampMainComponentUM.InitialLoading ->
            stateFactory.getReadyState(country.defaultCurrency, params.initialFiatAmount)
    }

    private fun subscribeToQuotesUpdate() {
        getOnrampQuotesUseCase.invoke()
            .conflate()
            .onEach { maybeQuotes ->
                maybeQuotes.fold(
                    ifLeft = ::handleOnrampError,
                    ifRight = ::handleQuoteResult,
                )
            }
            .launchIn(modelScope)
    }

    private fun handleQuoteResult(quotes: List<OnrampQuote>) {
        sendOnrampQuotesErrorAnalytic(quotes)
        when {
            quotes.isEmpty() -> {
                state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
            }
            quotes.all { it is OnrampQuote.AmountError } -> {
                state.update { amountStateFactory.getSecondaryFieldAmountErrorState(quotes) }
            }
            quotes.none { it is OnrampQuote.Data } -> {
                state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
            }
            else -> {
                analyticsEventHandler.sendProviderCalculatedEvent(
                    quotes = quotes,
                    tokenSymbol = params.cryptoCurrency.symbol,
                )
                state.update { prevState ->
                    val resetState = amountStateFactory.getAmountSecondaryFieldResetState()
                    if (prevState is OnrampMainComponentUM.Content &&
                        resetState is OnrampMainComponentUM.Content &&
                        prevState.offersBlockState is OnrampOffersBlockUM.Loading
                    ) {
                        resetState.copy(offersBlockState = OnrampOffersBlockUM.Empty)
                    } else {
                        resetState
                    }
                }
            }
        }
    }

    private fun onRetryQuotes() {
        state.update { prevState ->
            (prevState as? OnrampMainComponentUM.Content)?.copy(
                errorNotification = null,
                offersBlockState = OnrampOffersBlockUM.Loading,
                amountBlockState = prevState.amountBlockState.copy(
                    secondaryFieldModel = OnrampSecondaryFieldErrorUM.Empty,
                ),
            ) ?: prevState
        }
        startLoadingQuotes()
    }

    private suspend fun updatePairsAndQuotes() {
        fetchPairsUseCase.invoke(userWallet, params.cryptoCurrency).fold(
            ifLeft = ::handleOnrampError,
            ifRight = {
                state.update {
                    amountStateFactory.getAmountSecondaryFieldResetState()
                }
                startLoadingQuotes()
            },
        )
    }

    private suspend fun isTokenNotSupportedForBuy(): Boolean {
        // Token-level "cannot be bought", independent of country: the asset is either flagged as
        // not onrampable (BuyUnavailable) or absent from the express asset list (AssetNotFound).
        // Transient express states (loading/unreachable) are NOT treated as "not supported".
        val reason = rampStateManager.availableForBuy(
            userWallet = userWallet,
            cryptoCurrency = params.cryptoCurrency,
        )
        return reason is ScenarioUnavailabilityReason.BuyUnavailable ||
            reason is ScenarioUnavailabilityReason.AssetNotFound
    }

    private fun handleOnrampError(onrampError: OnrampError) {
        TangemLogger.e(onrampError.toString())
        state.update { stateFactory.getOnrampErrorState(onrampError) }
    }

    private fun showBuyNotSupported(country: OnrampCountry) {
        if (state.value.buyNotSupportedMessage != null) return

        analyticsEventHandler.send(
            OnrampAnalyticsEvent.NoticeBuyNotSupported(
                source = params.source,
                tokenSymbol = params.cryptoCurrency.symbol,
                blockchain = params.cryptoCurrency.network.name,
            ),
        )
        quotesTaskScheduler.cancelTask()
        // "Not supported" has priority: hide the residency bottom sheet if it was already shown.
        bottomSheetNavigation.dismiss()
        // Emit the not-supported state in a single update built from the ready state, so the amount
        // field never appears enabled first (no focus/keyboard flash).
        state.update { prevState ->
            stateFactory.getBuyNotSupportedState(getCountryUpdatedState(prevState, country))
        }
    }

    /** Converts the expected received [crypto] amount into its USD value using the target currency's USD rate. */
    private suspend fun computeAmountUsd(crypto: BigDecimal?): BigDecimal? {
        val amount = crypto ?: return null
        val rawCurrencyId = params.cryptoCurrency.id.rawCurrencyId ?: return null
        val rate = getCurrencyUSDQuoteUseCase(rawCurrencyId) ?: return null
        return amount * rate
    }

    private fun sendOnrampQuotesErrorAnalytic(quotes: List<OnrampQuote>) {
        quotes.forEach { errorState ->
            when (errorState) {
                is OnrampQuote.Error -> analyticsEventHandler.sendOnrampErrorEvent(
                    error = errorState.error,
                    tokenSymbol = params.cryptoCurrency.symbol,
                    providerName = errorState.provider.info.name,
                    paymentMethod = errorState.paymentMethod.name,
                )
                is OnrampQuote.AmountError -> analyticsEventHandler.sendOnrampErrorEvent(
                    error = errorState.error,
                    tokenSymbol = params.cryptoCurrency.symbol,
                    providerName = errorState.provider.info.name,
                    paymentMethod = errorState.paymentMethod.name,
                )
                else -> Unit
            }
        }
    }

    private fun sendScreenOpenAnalytics() {
        analyticsEventHandler.send(
            OnrampAnalyticsEvent.ScreenOpened(
                source = params.source,
                tokenSymbol = params.cryptoCurrency.symbol,
            ),
        )
    }

    private fun sendOfferClickEvent(
        quote: OnrampProviderWithQuote.Data,
        onrampOfferAdvantagesUM: OnrampOfferAdvantagesUM,
        categoryUM: OnrampOfferCategoryUM,
    ) {
        val event = when (categoryUM) {
            OnrampOfferCategoryUM.RecentlyUsed -> {
                OnrampAnalyticsEvent.RecentlyBuyClicked(
                    tokenSymbol = params.cryptoCurrency.symbol,
                    providerName = quote.provider.info.name,
                    paymentMethod = quote.paymentMethod.name,
                )
            }
            OnrampOfferCategoryUM.Recommended -> {
                onrampOfferAdvantagesUM.toAnalyticsEvent(
                    cryptoCurrencySymbol = params.cryptoCurrency.symbol,
                    providerName = quote.provider.info.name,
                    paymentMethodName = quote.paymentMethod.name,
                )
            }
        }

        if (event != null) {
            analyticsEventHandler.send(event)
        }
    }

    private companion object {
        const val UPDATE_DELAY = 10_000L
    }
}