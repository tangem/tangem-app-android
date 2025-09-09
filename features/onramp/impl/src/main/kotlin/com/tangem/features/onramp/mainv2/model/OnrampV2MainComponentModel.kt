package com.tangem.features.onramp.mainv2.model

import androidx.compose.runtime.mutableStateOf
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
import com.tangem.features.onramp.main.entity.OnrampIntents
import com.tangem.features.onramp.main.entity.OnrampLastUpdate
import com.tangem.features.onramp.mainv2.OnrampV2MainComponent
import com.tangem.features.onramp.mainv2.entity.*
import com.tangem.features.onramp.mainv2.entity.factory.OnrampAmountButtonUMStateFactory
import com.tangem.features.onramp.mainv2.entity.factory.OnrampOffersStateFactory
import com.tangem.features.onramp.mainv2.entity.factory.OnrampV2AmountStateFactory
import com.tangem.features.onramp.mainv2.entity.factory.OnrampV2StateFactory
import com.tangem.features.onramp.utils.sendOnrampErrorEvent
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.Job
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
) : Model(), OnrampIntents {

    val params = paramsContainer.require<OnrampV2MainComponent.Params>()

    private var loadQuotesJob: Job? = null

    private val lastUpdateState = mutableStateOf<OnrampLastUpdate?>(null)

    private val onrampAmountButtonUMStateFactory: OnrampAmountButtonUMStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampAmountButtonUMStateFactory()
    }

    private val onrampOffersStateFactory: OnrampOffersStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        OnrampOffersStateFactory(
            currentStateProvider = Provider { _state.value },
            onrampIntents = this,
        )
    }

    private val stateFactory = OnrampV2StateFactory(
        currentStateProvider = Provider { _state.value },
        cryptoCurrency = params.cryptoCurrency,
        onrampIntents = this,
        onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
    )

    private val amountStateFactory = OnrampV2AmountStateFactory(
        currentStateProvider = Provider { _state.value },
        analyticsEventHandler = analyticsEventHandler,
        onrampIntents = this,
        cryptoCurrency = params.cryptoCurrency,
        onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
    )

    private val _state: MutableStateFlow<OnrampV2MainComponentUM> = MutableStateFlow(
        value = stateFactory.getInitialState(
            currency = params.cryptoCurrency.name,
            onClose = ::onCloseClick,
            openSettings = ::openSettings,
        ),
    )
    val state: StateFlow<OnrampV2MainComponentUM> get() = _state.asStateFlow()
    val bottomSheetNavigation: SlotNavigation<OnrampV2MainBottomSheetConfig> = SlotNavigation()
    val userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }

    init {
        modelScope.launch {
            clearOnrampCacheUseCase()
        }

        sendScreenOpenAnalytics()
        checkResidenceCountry()
        subscribeToAmountChanges()
        subscribeToCountryAndCurrencyUpdates()
        subscribeToQuotesUpdate()
        subscribeOnOffers()
    }

    override fun onDestroy() {
        modelScope.launch { clearOnrampCacheUseCase.invoke() }
        loadQuotesJob?.cancel()
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

    override fun onBuyClick(quote: OnrampProviderWithQuote.Data) {
        val currentContentState = state.value as? OnrampV2MainComponentUM.Content ?: return
        analyticsEventHandler.send(
            OnrampAnalyticsEvent.OnBuyClick(
                providerName = quote.provider.info.name,
                currency = currentContentState.amountBlockState.currencyUM.code,
                tokenSymbol = params.cryptoCurrency.symbol,
            ),
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
        loadQuotesJob?.cancel()
        modelScope.launch {
            clearOnrampCacheUseCase.invoke()
            checkResidenceCountry()
        }
    }

    override fun onLinkClick(link: String) {
        // TODO in [REDACTED_TASK_KEY] will be deleted
    }

    override fun onContinueClick() {
        val currentState = _state.value
        if (currentState is OnrampV2MainComponentUM.Content) {
            _state.update { amountStateFactory.getShowProvidersState() }
        }
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
        getOnrampOffersUseCase.invoke(
            userWalletId = userWallet.walletId,
            cryptoCurrencyId = params.cryptoCurrency.id,
        ).collectLatest { maybeOffers ->
            maybeOffers.fold(
                ifLeft = ::handleOnrampError,
                ifRight = { offers ->
                    _state.update { onrampOffersStateFactory.getOnShowOffersState(offers) }
                },
            )
        }
    }

    private fun subscribeToAmountChanges() = modelScope.launch {
        amountInputManager.query
            .filter(String::isNotEmpty)
            .collectLatest { _ ->
                _state.update { amountStateFactory.getAmountSecondaryLoadingState() }
                loadQuotes()
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

        val quote = selectOrUpdateQuote(quotes)

        if (quote == null) {
            _state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
            lastUpdateState.value = null
            return
        }
        _state.update { amountStateFactory.getAmountSecondaryUpdatedState(quote = quote) }
    }

    private fun selectOrUpdateQuote(quotes: List<OnrampQuote>): OnrampQuote? {
        val quoteToCheck = quotes.firstOrNull { it !is OnrampQuote.Error }

        // Check if amount, country or currency has changed
        val newQuote = if (checkLastInputState(quoteToCheck)) {
            quoteToCheck
        } else {
            val state = state.value as? OnrampV2MainComponentUM.Content
            val providerState = state?.onrampProviderState as? OnrampV2ProvidersUM.Content

            // Get current selected quote to update
            val lastSelectedQuote = quotes.firstOrNull {
                it.provider.id == providerState?.providerId &&
                    it.paymentMethod.id == providerState.paymentMethod.id
            }

            if (lastSelectedQuote is OnrampQuote.Error) {
                quoteToCheck
            } else {
                lastSelectedQuote
            }
        }
        newQuote?.let { updateProvider(newQuote) }

        return newQuote
    }

    private fun onRetryQuotes() {
        _state.update {
            (it as? OnrampV2MainComponentUM.Content)?.copy(
                errorNotification = null,
                onrampProviderState = OnrampV2ProvidersUM.Loading,
                offersBlockState = OnrampOffersBlockUM.Loading(isBlockVisible = false),
                amountBlockState = it.amountBlockState.copy(
                    secondaryFieldModel = OnrampNewAmountSecondaryFieldUM.Loading,
                ),
            ) ?: it
        }
        loadQuotes()
    }

    private suspend fun updatePairsAndQuotes() {
        val state = state.value as? OnrampV2MainComponentUM.Content

        if (!state?.amountBlockState?.amountFieldModel?.fiatValue.isNullOrEmpty()) {
            _state.update { amountStateFactory.getAmountSecondaryLoadingState() }
        }
        fetchPairsUseCase.invoke(userWallet, params.cryptoCurrency).fold(
            ifLeft = ::handleOnrampError,
            ifRight = {
                _state.update {
                    if (!state?.amountBlockState?.amountFieldModel?.fiatValue.isNullOrEmpty()) {
                        return@fold
                    } else {
                        amountStateFactory.getAmountSecondaryResetState()
                    }
                }
            },
        )
        loadQuotes()
    }

    private fun loadQuotes() {
        loadQuotesJob?.cancel()
        loadQuotesJob = modelScope.launch {
            runCatching {
                val content = state.value as? OnrampV2MainComponentUM.Content ?: return@runCatching
                if (content.amountBlockState.amountFieldModel.fiatAmount.value.isNullOrZero()) return@runCatching
                fetchQuotesUseCase.invoke(
                    userWallet = userWallet,
                    amount = content.amountBlockState.amountFieldModel.fiatAmount,
                    cryptoCurrency = params.cryptoCurrency,
                ).onLeft(::handleOnrampError)
            }
        }
    }

    private fun handleOnrampError(onrampError: OnrampError) {
        Timber.e(onrampError.toString())
        _state.update { stateFactory.getOnrampErrorState(onrampError) }
    }

    private fun updateProvider(quote: OnrampQuote) {
        lastUpdateState.value = OnrampLastUpdate(
            quote.fromAmount,
            quote.countryCode,
        )

        _state.update {
            amountStateFactory.getUpdatedProviderState(selectedQuote = quote)
        }
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

    private fun checkLastInputState(quote: OnrampQuote?): Boolean {
        return lastUpdateState.value?.lastAmount != quote?.fromAmount ||
            lastUpdateState.value?.lastCountryString != quote?.countryCode
    }

    private fun sendScreenOpenAnalytics() {
        analyticsEventHandler.send(
            OnrampAnalyticsEvent.ScreenOpened(
                source = params.source,
                tokenSymbol = params.cryptoCurrency.symbol,
            ),
        )
    }
}