package com.tangem.features.onramp.main.model

import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.message.ContentMessage
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.*
import com.tangem.features.onramp.main.entity.factory.OnrampStateFactory
import com.tangem.features.onramp.main.entity.factory.amount.OnrampAmountStateFactory
import com.tangem.features.onramp.providers.entity.SelectProviderResult
import com.tangem.features.onramp.utils.InputManager
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
internal class OnrampMainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    private val clearOnrampCacheUseCase: ClearOnrampCacheUseCase,
    private val fetchQuotesUseCase: OnrampFetchQuotesUseCase,
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase,
    private val fetchPairsUseCase: OnrampFetchPairsUseCase,
    private val amountInputManager: InputManager,
    private val messageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    getWalletsUseCase: GetWalletsUseCase,
    paramsContainer: ParamsContainer,
) : Model(), OnrampIntents {

    private val params: OnrampMainComponent.Params = paramsContainer.require()
    private val stateFactory = OnrampStateFactory(
        currentStateProvider = Provider { _state.value },
        cryptoCurrency = params.cryptoCurrency,
        onrampIntents = this,
    )
    private val amountStateFactory = OnrampAmountStateFactory(
        currentStateProvider = Provider { _state.value },
        analyticsEventHandler = analyticsEventHandler,
        onrampIntents = this,
        cryptoCurrency = params.cryptoCurrency,
    )
    private val selectedUserWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }
    private val _state: MutableStateFlow<OnrampMainComponentUM> = MutableStateFlow(
        value = stateFactory.getInitialState(
            currency = params.cryptoCurrency.name,
            onClose = ::onCloseClick,
        ),
    )
    private val quotesTaskScheduler = SingleTaskScheduler<Unit>()
    val state: StateFlow<OnrampMainComponentUM> get() = _state.asStateFlow()
    val bottomSheetNavigation: SlotNavigation<OnrampMainBottomSheetConfig> = SlotNavigation()

    private val lastUpdateState = mutableStateOf<OnrampLastUpdate?>(null)

    init {
        sendScreenOpenAnalytics()
        checkResidenceCountry()
        subscribeToAmountChanges()
    }

    private fun sendScreenOpenAnalytics() {
        analyticsEventHandler.send(
            OnrampAnalyticsEvent.ScreenOpened(
                source = params.source,
                tokenSymbol = params.cryptoCurrency.symbol,
            ),
        )
    }

    fun handleOnrampAvailable() {
        subscribeToCountryAndCurrencyUpdates()
        subscribeToQuotesUpdate()
    }

    fun onProviderSelected(result: SelectProviderResult, isBestRate: Boolean) {
        _state.update { amountStateFactory.getAmountSecondaryUpdatedState(result, isBestRate) }
    }

    private fun checkResidenceCountry() {
        modelScope.launch {
            checkOnrampAvailabilityUseCase()
                .onRight(::handleOnrampAvailability)
                .onLeft(::handleOnrampError)
        }
    }

    private fun handleOnrampAvailability(availability: OnrampAvailability) {
        when (availability) {
            is OnrampAvailability.Available -> handleOnrampAvailable()
            is OnrampAvailability.ConfirmResidency,
            is OnrampAvailability.NotSupported,
            -> bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.ConfirmResidency(availability.country))
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
                            if (it is OnrampMainComponentUM.InitialLoading) {
                                stateFactory.getReadyState(country.defaultCurrency)
                            } else {
                                amountStateFactory.getUpdatedCurrencyState(country.defaultCurrency)
                            }
                        }
                        updatePairsAndQuotes()
                    },
                )
            }
            .launchIn(modelScope)
    }

    private fun subscribeToAmountChanges() = modelScope.launch {
        amountInputManager.query
            .filter(String::isNotEmpty)
            .collectLatest { _ ->
                _state.update { amountStateFactory.getAmountSecondaryLoadingState() }
                startLoadingQuotes()
            }
    }

    private suspend fun updatePairsAndQuotes() {
        val state = state.value as? OnrampMainComponentUM.Content

        if (!state?.amountBlockState?.amountFieldModel?.fiatValue.isNullOrEmpty()) {
            _state.update { amountStateFactory.getAmountSecondaryLoadingState() }
        }
        fetchPairsUseCase.invoke(params.cryptoCurrency).fold(
            ifLeft = ::handleOnrampError,
            ifRight = { _state.update { amountStateFactory.getAmountSecondaryResetState() } },
        )
        startLoadingQuotes()
    }

    private fun handleOnrampError(onrampError: OnrampError) {
        Timber.e(onrampError.toString())
        sendOnrampErrorAnalytic(onrampError)
        _state.update { stateFactory.getOnrampErrorState(onrampError) }
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
                    val content = state.value as? OnrampMainComponentUM.Content ?: return@runCatching
                    if (content.amountBlockState.amountFieldModel.fiatAmount.value.isNullOrZero()) return@runCatching
                    fetchQuotesUseCase.invoke(
                        amount = content.amountBlockState.amountFieldModel.fiatAmount,
                        cryptoCurrency = params.cryptoCurrency,
                    ).onLeft(::handleOnrampError)
                }
            },
            onSuccess = {},
            onError = {},
        )
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

    override fun onAmountValueChanged(value: String) {
        _state.update { amountStateFactory.getOnAmountValueChange(value) }
        modelScope.launch { amountInputManager.update(value) }
    }

    override fun openSettings() {
        params.openSettings()
    }

    override fun onBuyClick(quote: OnrampProviderWithQuote.Data) {
        if (isDemoCardUseCase.invoke(selectedUserWallet.cardId)) {
            showDemoWarning()
        } else {
            val currentContentState = state.value as? OnrampMainComponentUM.Content ?: return
            analyticsEventHandler.send(
                OnrampAnalyticsEvent.OnBuyClick(
                    providerName = quote.provider.info.name,
                    currency = currentContentState.amountBlockState.currencyUM.code,
                    tokenSymbol = params.cryptoCurrency.symbol,
                ),
            )
            params.openRedirectPage(quote)
        }
    }

    override fun openCurrenciesList() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.SelectCurrencyScreenOpened)
        bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.CurrenciesList)
    }

    override fun openProviders() {
        val providerState = (state.value as? OnrampMainComponentUM.Content)?.providerBlockState ?: return
        val providerContentState = providerState as? OnrampProviderBlockUM.Content ?: return
        bottomSheetNavigation.activate(
            OnrampMainBottomSheetConfig.ProvidersList(
                selectedPaymentMethod = providerContentState.paymentMethod,
                selectedProviderId = providerContentState.providerId,
            ),
        )
    }

    override fun onRefresh() {
        _state.update {
            stateFactory.getInitialState(
                currency = params.cryptoCurrency.name,
                onClose = router::pop,
            )
        }
        quotesTaskScheduler.cancelTask()
        modelScope.launch {
            clearOnrampCacheUseCase.invoke()
            checkResidenceCountry()
        }
    }

    override fun onLinkClick(link: String) = urlOpener.openUrl(link)

    override fun onDestroy() {
        modelScope.launch { clearOnrampCacheUseCase.invoke() }
        quotesTaskScheduler.cancelTask()
        super.onDestroy()
    }

    private fun onCloseClick() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.CloseOnramp)
        router.pop()
    }

    private fun handleQuoteResult(quotes: List<OnrampQuote>) {
        sendOnrampQuotesErrorAnalytic(quotes)

        val quote = selectOrUpdateQuote(quotes)

        if (quote == null) {
            _state.update { stateFactory.getErrorState(onRefresh = ::onRetryQuotes) }
            lastUpdateState.value = null
            return
        }

        if (checkLastInputState(quote)) {
            lastUpdateState.value = OnrampLastUpdate(
                quote.fromAmount,
                quote.countryCode,
            )

            _state.update {
                amountStateFactory.getUpdatedProviderState(selectedQuote = quote, quotes = quotes)
            }
        }
        _state.update { amountStateFactory.getAmountSecondaryUpdatedState(quote = quote) }
    }

    /**
     * !!! Important quote selection logic !!!
     * Selects or updated quote based on input data (amount, country, currency).
     * If input data has changed select new best quote, otherwise last selected quote.
     * If last selected quote on same input data is in an error state, select next best quote
     * If new best quote or next best quote does not exist (i.e. Error state) select nothing.
     */
    private fun selectOrUpdateQuote(quotes: List<OnrampQuote>): OnrampQuote? {
        val quoteToCheck = quotes.firstOrNull { it !is OnrampQuote.Error }

        // Check if amount, country or currency has changed
        return if (checkLastInputState(quoteToCheck)) {
            quoteToCheck
        } else {
            val state = state.value as? OnrampMainComponentUM.Content
            val providerState = state?.providerBlockState as? OnrampProviderBlockUM.Content

            // Get current selected quote to update
            val lastSelectedQuote = quotes.firstOrNull {
                it.provider.id == providerState?.providerId &&
                    it.paymentMethod.id == providerState.paymentMethod.id
            }

            // Check if selected updated quote is not error
            if (lastSelectedQuote is OnrampQuote.Error) {
                quoteToCheck
            } else {
                lastSelectedQuote
            }
        }
    }

    private fun onRetryQuotes() {
        _state.update {
            (it as? OnrampMainComponentUM.Content)?.copy(
                errorNotification = null,
                providerBlockState = OnrampProviderBlockUM.Loading,
            ) ?: it
        }
        startLoadingQuotes()
    }

    private fun showDemoWarning() {
        val alertUM = AlertDemoModeUM(onConfirmClick = {})

        messageSender.send(
            message = ContentMessage { onDismiss ->
                val confirmButton = DialogButtonUM(
                    title = alertUM.confirmButtonText.resolveReference(),
                    onClick = {
                        alertUM.onConfirmClick()
                        onDismiss()
                    },
                )

                val dismissButton = DialogButtonUM(
                    title = stringResourceSafe(id = R.string.common_cancel),
                    onClick = onDismiss,
                )

                BasicDialog(
                    message = alertUM.message.resolveReference(),
                    confirmButton = confirmButton,
                    onDismissDialog = onDismiss,
                    title = alertUM.title.resolveReference(),
                    dismissButton = dismissButton,
                )
            },
        )
    }

    private fun sendOnrampErrorAnalytic(error: OnrampError) {
        val content = state.value as? OnrampMainComponentUM.Content
        val providerContent = content?.providerBlockState as? OnrampProviderBlockUM.Content
        analyticsEventHandler.sendOnrampErrorEvent(
            error = error,
            tokenSymbol = params.cryptoCurrency.symbol,
            providerName = providerContent?.providerName,
            paymentMethod = providerContent?.paymentMethod?.name,
        )
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
                else -> { /* no-op */ }
            }
        }
    }

    private fun checkLastInputState(quote: OnrampQuote?): Boolean {
        return lastUpdateState.value?.lastAmount != quote?.fromAmount ||
            lastUpdateState.value?.lastCountryString != quote?.countryCode
    }

    private companion object {
        const val UPDATE_DELAY = 10_000L
    }
}