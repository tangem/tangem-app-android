package com.tangem.features.onramp.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.deeplink.MarketingDeeplink
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
import com.tangem.domain.marketing.ObserveDemandSurveyCompletedUseCase
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.model.isRestricted
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.marketing.api.CtaBannerRequest
import com.tangem.features.marketing.api.LinkedBannerRequest
import com.tangem.features.marketing.api.MarketingBannerAmount
import com.tangem.features.marketing.api.MarketingBannerRequest
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
import com.tangem.features.onramp.deeplink.ConciergeWaitlistListener
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
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
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.isNullOrZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
internal class OnrampMainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    private val clearOnrampCacheUseCase: ClearOnrampCacheUseCase,
    private val resetOnrampCountryUseCase: ResetOnrampCountryUseCase,
    private val fetchQuotesUseCase: OnrampFetchQuotesUseCase,
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase,
    private val fetchPairsUseCase: OnrampFetchPairsUseCase,
    private val rampStateManager: RampStateManager,
    private val amountInputManager: InputManager,
    private val getOnrampOffersUseCase: GetOnrampOffersUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val messageSender: UiMessageSender,
    private val conciergeWaitlistListener: ConciergeWaitlistListener,
    private val observeDemandSurveyCompletedUseCase: ObserveDemandSurveyCompletedUseCase,
    private val onrampFeatureToggles: OnrampFeatureToggles,
    private val appCoroutineScope: AppCoroutineScope,
    paramsContainer: ParamsContainer,
    getWalletsUseCase: GetWalletsUseCase,
) : Model(), OnrampIntents {

    val params = paramsContainer.require<OnrampMainComponent.Params>()

    private val isCountryByIpEnabled: Boolean = onrampFeatureToggles.isCountryByIpEnabled

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
            isNewDesignSystemEnabled = Provider { onrampFeatureToggles.isNewDesignSystemEnabled },
        )
    }

    val state: StateFlow<OnrampMainComponentUM>
        field = MutableStateFlow<OnrampMainComponentUM>(
            value = stateFactory.getInitialState(
                currency = params.cryptoCurrency.name,
                onClose = ::onCloseClick,
                openSettings = ::openSettings,
                isCountryByIpEnabled = isCountryByIpEnabled,
            ),
        )

    /**
     * Request flow for the standalone marketing banner.
     * [fromFiat] is the fiat currency code the user is paying in (only available once the screen is in Content state).
     * [toNetwork] is the backend network id of the target crypto currency.
     * [toContractAddress] is the contract address of the target token (empty string for coins).
     * [amount] is the typed fiat amount (see [toMarketingAmount]); the banner converts it to USD for the campaigns'
     * min/max bounds, so the bounds also apply when the amount is outside every provider's limits.
     */
    val marketingRequest: Flow<MarketingBannerRequest?> = state.map { s ->
        val contentState = s as? OnrampMainComponentUM.Content ?: return@map null
        MarketingBannerRequest(
            screen = MarketingScreen.Onramp(
                fromFiat = contentState.amountBlockState.currencyUM.code,
                toNetwork = params.cryptoCurrency.network.rawId,
                toContractAddress = (params.cryptoCurrency as? CryptoCurrency.Token)?.contractAddress.orEmpty(),
            ),
            amount = contentState.amountBlockState.toMarketingAmount(),
        )
    }

    /**
     * Request flow for the LINKED_TO_PROVIDER marketing banner shown inline next to onramp provider offers.
     * Carries no provider id: provider matching is done per offer at render time (each offer row asks for its
     * banner via [MarketingBannerComponent.LinkedContent]), mirroring iOS.
     * [amount] follows the same rules as in [marketingRequest].
     */
    val linkedMarketingRequest: Flow<LinkedBannerRequest?> = state.map { s ->
        val contentState = s as? OnrampMainComponentUM.Content ?: return@map null
        LinkedBannerRequest(
            screen = MarketingScreen.Onramp(
                fromFiat = contentState.amountBlockState.currencyUM.code,
                toNetwork = params.cryptoCurrency.network.rawId,
                toContractAddress = (params.cryptoCurrency as? CryptoCurrency.Token)?.contractAddress.orEmpty(),
            ),
            amount = contentState.amountBlockState.toMarketingAmount(),
        )
    }

    /**
     * Request flow for the PSEUDO_PROVIDER / STANDALONE_CTA banners above the payment methods. The demand survey
     * status is requested once per Buy session, not per amount change. Until it arrives the Waitlist strip stays
     * hidden, so a wallet that has answered never sees it flash; a failed request reports `false` and shows it.
     * Shared because the banner collects the request flow more than once and each collection requests the status.
     */
    val ctaMarketingRequest: Flow<CtaBannerRequest?> = combine(
        marketingRequest,
        observeDemandSurveyCompletedUseCase(params.userWalletId)
            .onStart { emit(true) }
            .shareIn(scope = modelScope, started = SharingStarted.Lazily, replay = 1),
    ) { request, isDemandSurveyCompleted ->
        val screen = request?.screen as? MarketingScreen.Onramp ?: return@combine null
        CtaBannerRequest(
            screen = screen,
            amount = request.amount,
            isDemandSurveyCompleted = isDemandSurveyCompleted,
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
    private var sentGeoRestrictionProviders: Set<String> = emptySet()
    private val conciergeWaitlistJobHolder = JobHolder()
    private val countrySubscriptionJobHolder = JobHolder()
    private var isCountryBlocked = false

    val bottomSheetNavigation: SlotNavigation<OnrampMainBottomSheetConfig> = SlotNavigation()
    val demandSurveyNavigation: SlotNavigation<ConciergeDemandSurveyComponent.Params> = SlotNavigation()
    val userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }

    init {
        startLoadingQuotes()
        sendScreenOpenAnalytics()
        subscribeToAmountChanges()
        subscribeToQuotesUpdate()
        subscribeOnOffers()
        if (isCountryByIpEnabled) {
            modelScope.launch {
                clearOnrampCacheUseCase()
                resetOnrampCountryUseCase()
                checkResidenceCountry()
                subscribeToCountryAndCurrencyUpdates()
            }
        } else {
            modelScope.launch { clearOnrampCacheUseCase() }
            checkResidenceCountry()
            subscribeToCountryAndCurrencyUpdates()
        }
    }

    override fun onDestroy() {
        appCoroutineScope.launch {
            if (isCountryByIpEnabled) resetOnrampCountryUseCase()
            clearOnrampCacheUseCase.invoke()
        }
        quotesTaskScheduler.cancelTask()
        super.onDestroy()
    }

    fun onMarketingBannerDeeplink(deeplink: String): Boolean {
        val marketing = resolveMarketingDeeplink(deeplink)
        if (marketing == MarketingDeeplink.SWAP) return false
        val route = marketing.toContextualRoute(
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
        if (isCountryByIpEnabled) {
            bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.SelectCountry)
        } else {
            params.openSettings.invoke()
        }
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
        if (isCountryBlocked ||
            onrampOfferAdvantagesUM is OnrampOfferAdvantagesUM.Unavailable ||
            onrampOfferAdvantagesUM == OnrampOfferAdvantagesUM.Restricted
        ) {
            return
        }
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
                isCountryByIpEnabled = isCountryByIpEnabled,
            )
        }
        isCountryBlocked = false
        modelScope.launch {
            clearOnrampCacheUseCase.invoke()
            checkAvailability()
            handleOnrampAvailable()
        }
    }

    fun onStart() {
        conciergeWaitlistListener.openWaitlistFlow
            .onEach { openDemandSurvey() }
            .launchIn(modelScope)
            .saveIn(conciergeWaitlistJobHolder)
    }

    // The survey would render under the sheets' dialog windows; residency confirmation and country selection
    // keep priority over the link.
    private fun openDemandSurvey() {
        bottomSheetNavigation.navigate(
            transformer = { sheet ->
                sheet.takeIf { config ->
                    config is OnrampMainBottomSheetConfig.ConfirmResidency ||
                        config is OnrampMainBottomSheetConfig.SelectCountry
                }
            },
            onComplete = { sheet, _ ->
                if (sheet == null) {
                    demandSurveyNavigation.activate(ConciergeDemandSurveyComponent.Params(params.userWalletId))
                }
            },
        )
    }

    fun onStop() {
        quotesTaskScheduler.cancelTask()
        conciergeWaitlistJobHolder.cancel()
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
        modelScope.launch { checkAvailability() }
    }

    private suspend fun checkAvailability() {
        checkOnrampAvailabilityUseCase(userWallet, isCountryByIpEnabled)
            .onLeft(::handleOnrampError)
            .onRight { availability ->
                if (isCountryByIpEnabled) {
                    if (availability is OnrampAvailability.NotSupported) {
                        showCountryNotSupported(availability.country)
                    }
                } else {
                    handleOnrampAvailability(availability)
                }
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
                        if (isCountryBlocked) return@fold
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
                        if (isCountryByIpEnabled) {
                            state.update { stateFactory.getCountryFlagState(it, country.image, country.name) }
                        }
                        if (isCountryByIpEnabled && !country.isOnrampAvailable) {
                            showCountryNotSupported(country)
                            return@onEach
                        }
                        if (isCountryBlocked) {
                            onRefresh()
                            return@onEach
                        }
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
            .saveIn(countrySubscriptionJobHolder)
    }

    private fun getCountryUpdatedState(
        prevState: OnrampMainComponentUM,
        country: OnrampCountry,
    ): OnrampMainComponentUM {
        val updatedState = when (prevState) {
            is OnrampMainComponentUM.Content -> amountStateFactory.getUpdatedCurrencyState(country.defaultCurrency)
            is OnrampMainComponentUM.InitialLoading ->
                stateFactory.getReadyState(country.defaultCurrency, params.initialFiatAmount)
        }
        return if (isCountryByIpEnabled) {
            stateFactory.getCountryFlagState(
                state = updatedState,
                flagUrl = country.image,
                countryName = country.name,
            )
        } else {
            updatedState
        }
    }

    /**
     * Every re-subscription follows a teardown of the current screen state — a refresh, or the residency
     * sheet being dismissed — so the restriction notice is gone by the time quotes arrive again and the
     * next one is a new appearance rather than a repeat of the reported one.
     */
    private fun subscribeToQuotesUpdate() {
        sentGeoRestrictionProviders = emptySet()
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
        if (isCountryBlocked) return
        sendOnrampQuotesErrorAnalytic(quotes)
        val hasAvailableOffer = quotes.any { it is OnrampQuote.Data && !it.isRestricted }
        val isRegionRestricted = !hasAvailableOffer && quotes.any(OnrampQuote::isRestricted)
        sendGeoRestrictionAnalytic(if (isRegionRestricted) quotes else emptyList())

        when {
            quotes.isEmpty() -> {
                state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
            }
            quotes.all { it is OnrampQuote.AmountError } -> {
                state.update { amountStateFactory.getSecondaryFieldAmountErrorState(quotes) }
            }
            !hasAvailableOffer -> {
                if (isRegionRestricted) {
                    state.update { amountStateFactory.getSecondaryFieldRestrictedErrorState() }
                } else {
                    state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
                }
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

    private fun showCountryNotSupported(country: OnrampCountry) {
        isCountryBlocked = true
        quotesTaskScheduler.cancelTask()
        state.update { prevState ->
            stateFactory.getCountryNotSupportedState(getCountryUpdatedState(prevState, country))
        }
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
        bottomSheetNavigation.dismiss()
        // Emit the not-supported state in a single update built from the ready state, so the amount
        // field never appears enabled first (no focus/keyboard flash).
        state.update { prevState ->
            stateFactory.getBuyNotSupportedState(getCountryUpdatedState(prevState, country))
        }
    }

    /** Null while the field is empty; a typed `0` is still an amount. */
    private fun OnrampAmountBlockUM.toMarketingAmount(): MarketingBannerAmount.Fiat? {
        if (amountFieldModel.fiatValue.isEmpty()) return null
        val value = amountFieldModel.fiatAmount.value ?: return null
        return MarketingBannerAmount.Fiat(value = value, currencyCode = currencyUM.code)
    }

    /** Reports every newly restricted provider once; an empty [quotes] re-arms the guard. */
    private fun sendGeoRestrictionAnalytic(quotes: List<OnrampQuote>) {
        val restrictedProviders = quotes.filter(OnrampQuote::isRestricted)
            .map { it.provider.info.name }
            .toSet()

        (restrictedProviders - sentGeoRestrictionProviders).forEach { providerName ->
            analyticsEventHandler.send(
                OnrampAnalyticsEvent.NoticeGeoRestriction(
                    tokenSymbol = params.cryptoCurrency.symbol,
                    providerName = providerName,
                ),
            )
        }
        sentGeoRestrictionProviders = restrictedProviders
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