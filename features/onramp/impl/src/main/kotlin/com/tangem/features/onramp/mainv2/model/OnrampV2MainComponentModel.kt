package com.tangem.features.onramp.mainv2.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.mainv2.OnrampV2MainComponent
import com.tangem.features.onramp.mainv2.entity.*
import com.tangem.features.onramp.mainv2.entity.factory.OnrampAmountButtonUMStateFactory
import com.tangem.features.onramp.mainv2.entity.factory.OnrampOffersStateFactory
import com.tangem.features.onramp.mainv2.entity.factory.OnrampV2AmountStateFactory
import com.tangem.features.onramp.mainv2.entity.factory.OnrampV2StateFactory
import com.tangem.features.onramp.utils.sendOnrampErrorEvent
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
internal class OnrampV2MainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    private val clearOnrampCacheUseCase: ClearOnrampCacheUseCase,
    private val fetchQuotesUseCase: OnrampFetchQuotesUseCase,
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase,
    private val fetchPairsUseCase: OnrampFetchPairsUseCase,
    private val amountInputManager: InputManager,
    private val getOnrampOffersUseCase: GetOnrampOffersUseCase,
    paramsContainer: ParamsContainer,
    getWalletsUseCase: GetWalletsUseCase,
) : Model(), OnrampV2Intents {

    val params = paramsContainer.require<OnrampV2MainComponent.Params>()

    private val onrampAmountButtonUMStateFactory: OnrampAmountButtonUMStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampAmountButtonUMStateFactory()
    }

    private val onrampOffersStateFactory: OnrampOffersStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampOffersStateFactory(
            currentStateProvider = Provider { _state.value },
            onrampIntents = this,
        )
    }

    private val stateFactory: OnrampV2StateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampV2StateFactory(
            currentStateProvider = Provider { _state.value },
            cryptoCurrency = params.cryptoCurrency,
            onrampIntents = this,
            onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
        )
    }

    private val amountStateFactory: OnrampV2AmountStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampV2AmountStateFactory(
            currentStateProvider = Provider { _state.value },
            analyticsEventHandler = analyticsEventHandler,
            onrampIntents = this,
            onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
        )
    }

    private val _state: MutableStateFlow<OnrampV2MainComponentUM> = MutableStateFlow(
        value = stateFactory.getInitialState(
            currency = params.cryptoCurrency.name,
            onClose = ::onCloseClick,
            openSettings = ::openSettings,
        ),
    )
    private val quotesTaskScheduler = SingleTaskScheduler<Unit>()

    val state: StateFlow<OnrampV2MainComponentUM> get() = _state.asStateFlow()
    val bottomSheetNavigation: SlotNavigation<OnrampV2MainBottomSheetConfig> = SlotNavigation()
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

    override fun onAmountValueChanged(value: String) {
        _state.update { amountStateFactory.getOnAmountValueChange(value) }
        modelScope.launch { amountInputManager.update(value) }
    }

    override fun openSettings() {
        params.openSettings.invoke()
    }

    override fun openCurrenciesList() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.SelectCurrencyScreenOpened)
        bottomSheetNavigation.activate(OnrampV2MainBottomSheetConfig.CurrenciesList)
    }

    override fun onBuyClick(
        quote: OnrampProviderWithQuote.Data,
        onrampOfferAdvantagesUM: OnrampOfferAdvantagesUM,
        categoryUM: OnrampOfferCategoryUM,
    ) {
        val currentContentState = state.value as? OnrampV2MainComponentUM.Content ?: return
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
        params.openRedirectPage(quote)
    }

    override fun openProviders() {
        val currentContentState = state.value as? OnrampV2MainComponentUM.Content ?: return
        val amountCurrentCode = currentContentState.amountBlockState.currencyUM.code
        bottomSheetNavigation.activate(OnrampV2MainBottomSheetConfig.AllOffers(amountCurrentCode))
    }

    override fun onRefresh() {
        _state.update {
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
                runCatching {
                    val content = state.value as? OnrampV2MainComponentUM.Content ?: return@runCatching
                    if (content.amountBlockState.amountFieldModel.fiatAmount.value.isNullOrZero()) return@runCatching
                    fetchQuotesUseCase.invoke(
                        userWallet = userWallet,
                        amount = content.amountBlockState.amountFieldModel.fiatAmount,
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
        when (availability) {
            is OnrampAvailability.Available -> Unit
            is OnrampAvailability.ConfirmResidency,
            is OnrampAvailability.NotSupported,
            -> bottomSheetNavigation.activate(OnrampV2MainBottomSheetConfig.ConfirmResidency(availability.country))
        }
    }

    private fun onCloseClick() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.CloseOnramp)
        router.pop()
    }

    private fun subscribeOnOffers() = modelScope.launch {
        getOnrampOffersUseCase
            .invoke()
            .collectLatest { maybeOffers ->
                maybeOffers.fold(
                    ifLeft = ::handleOnrampError,
                    ifRight = { offers ->
                        if (offers.isNotEmpty()) {
                            _state.update { onrampOffersStateFactory.getOffersState(offers) }
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
                        _state.update {
                            when (it) {
                                is OnrampV2MainComponentUM.Content -> {
                                    amountStateFactory.getUpdatedCurrencyState(country.defaultCurrency)
                                }
                                is OnrampV2MainComponentUM.InitialLoading -> {
                                    stateFactory.getReadyState(country.defaultCurrency)
                                }
                            }
                        }
                        updatePairsAndQuotes()
                    },
                )
            }
            .launchIn(modelScope)
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
                _state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
            }
            quotes.all { it is OnrampQuote.AmountError } -> {
                _state.update { amountStateFactory.getSecondaryFieldAmountErrorState(quotes) }
            }
            quotes.none { it is OnrampQuote.Data } -> {
                _state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
            }
            else -> {
                _state.update { amountStateFactory.getAmountSecondaryFieldResetState() }
            }
        }
    }

    private fun onRetryQuotes() {
        _state.update {
            (it as? OnrampV2MainComponentUM.Content)?.copy(
                errorNotification = null,
                offersBlockState = OnrampOffersBlockUM.Loading,
                amountBlockState = it.amountBlockState.copy(secondaryFieldModel = OnrampSecondaryFieldErrorUM.Empty),
            ) ?: it
        }
        startLoadingQuotes()
    }

    private suspend fun updatePairsAndQuotes() {
        fetchPairsUseCase.invoke(userWallet, params.cryptoCurrency).fold(
            ifLeft = ::handleOnrampError,
            ifRight = {
                _state.update {
                    amountStateFactory.getAmountSecondaryFieldResetState()
                }
                startLoadingQuotes()
            },
        )
    }

    private fun handleOnrampError(onrampError: OnrampError) {
        Timber.e(onrampError.toString())
        _state.update { stateFactory.getOnrampErrorState(onrampError) }
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
        when (categoryUM) {
            OnrampOfferCategoryUM.RecentlyUsed -> {
                OnrampAnalyticsEvent.RecentlyBuyClicked(
                    tokenSymbol = params.cryptoCurrency.symbol,
                    providerName = quote.provider.info.name,
                    paymentMethod = quote.paymentMethod.name,
                ).let(analyticsEventHandler::send)
            }
            OnrampOfferCategoryUM.Recommended -> {
                onrampOfferAdvantagesUM.toAnalyticsEvent(
                    cryptoCurrencySymbol = params.cryptoCurrency.symbol,
                    providerName = quote.provider.info.name,
                    paymentMethodName = quote.paymentMethod.name,
                )?.let(analyticsEventHandler::send)
            }
        }
    }

    private companion object {
        const val UPDATE_DELAY = 10_000L
    }
}