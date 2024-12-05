package com.tangem.features.onramp.main.model

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.message.ContentMessage
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.OnrampIntents
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampProviderBlockUM
import com.tangem.features.onramp.main.entity.factory.OnrampStateFactory
import com.tangem.features.onramp.main.entity.factory.amount.OnrampAmountStateFactory
import com.tangem.features.onramp.utils.InputManager
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
internal class OnrampMainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    private val getOnrampCurrencyUseCase: GetOnrampCurrencyUseCase,
    private val clearOnrampCacheUseCase: ClearOnrampCacheUseCase,
    private val fetchQuotesUseCase: OnrampFetchQuotesUseCase,
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase,
    private val fetchPairsUseCase: OnrampFetchPairsUseCase,
    private val amountInputManager: InputManager,
    private val messageSender: UiMessageSender,
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
        onrampIntents = this,
    )
    private val selectedUserWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }
    private val _state: MutableStateFlow<OnrampMainComponentUM> = MutableStateFlow(
        value = stateFactory.getInitialState(
            currency = params.cryptoCurrency.name,
            onClose = router::pop,
        ),
    )
    private val quotesTaskScheduler = SingleTaskScheduler<Unit>()
    val state: StateFlow<OnrampMainComponentUM> get() = _state.asStateFlow()
    val bottomSheetNavigation: SlotNavigation<OnrampMainBottomSheetConfig> = SlotNavigation()

    private val lastAmount = mutableStateOf(BigDecimal.ZERO)

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

    fun handleOnrampAvailable(currency: OnrampCurrency) {
        _state.update { stateFactory.getReadyState(currency) }
        subscribeToCurrencyUpdates()
        subscribeToQuotesUpdate()
    }

    fun onProviderSelected(providerWithQuote: OnrampProviderWithQuote.Data, isBestRate: Boolean) {
        _state.update { amountStateFactory.getAmountSecondaryUpdatedState(providerWithQuote, isBestRate) }
    }

    private fun checkResidenceCountry() {
        modelScope.launch {
            checkOnrampAvailabilityUseCase.invoke(params.cryptoCurrency)
                .onRight(::handleOnrampAvailability)
                .onLeft { Timber.e(it) }
        }
    }

    private fun handleOnrampAvailability(availability: OnrampAvailability) {
        when (availability) {
            is OnrampAvailability.Available -> handleOnrampAvailable(availability.currency)
            is OnrampAvailability.ConfirmResidency,
            is OnrampAvailability.NotSupported,
            -> bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.ConfirmResidency(availability.country))
        }
    }

    private fun subscribeToCurrencyUpdates() {
        getOnrampCurrencyUseCase.invoke()
            .onEach { maybeCurrency ->
                val currency = maybeCurrency.getOrNull()
                if (currency != null) {
                    _state.update { amountStateFactory.getUpdatedCurrencyState(currency) }
                    updatePairsAndQuotes()
                }
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
        fetchPairsUseCase.invoke(params.cryptoCurrency)
        startLoadingQuotes()
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
                    )
                }
            },
            onSuccess = {},
            onError = {},
        )
    }

    private fun subscribeToQuotesUpdate() {
        getOnrampQuotesUseCase.invoke()
            .onEach { maybeQuotes ->
                val quotes = maybeQuotes.getOrNull() ?: return@onEach
                val isNoQuotes = quotes.none { it is OnrampQuote.Data }
                val quote = quotes.firstOrNull() ?: return@onEach
                if (quote is OnrampQuote.Data && lastAmount.value != quote.fromAmount.value) {
                    lastAmount.value = quote.fromAmount.value
                    analyticsEventHandler.send(
                        OnrampAnalyticsEvent.ProviderCalculated(
                            providerName = quote.provider.info.name,
                            tokenSymbol = params.cryptoCurrency.symbol,
                            paymentMethod = quote.paymentMethod.name,
                        ),
                    )
                }
                _state.update { amountStateFactory.getAmountSecondaryUpdatedState(quote, isNoQuotes) }
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
            ),
        )
    }

    override fun onDestroy() {
        modelScope.launch { clearOnrampCacheUseCase.invoke() }
        quotesTaskScheduler.cancelTask()
        super.onDestroy()
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
                    title = stringResource(id = R.string.common_cancel),
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

    private companion object {
        const val UPDATE_DELAY = 10_000L
    }
}