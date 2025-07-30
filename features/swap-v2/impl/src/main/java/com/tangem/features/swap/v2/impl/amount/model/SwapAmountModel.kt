package com.tangem.features.swap.v2.impl.amount.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapDirection.Companion.withSwapDirection
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.domain.swap.models.getGroupWithDirection
import com.tangem.domain.swap.usecase.GetSwapQuoteUseCase
import com.tangem.domain.swap.usecase.SelectInitialPairUseCase
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceUseCase
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkListener
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.SwapAmountBlockComponent.SwapChooseProviderConfig
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponentParams
import com.tangem.features.swap.v2.impl.amount.SwapAmountReduceListener
import com.tangem.features.swap.v2.impl.amount.SwapAmountUpdateListener
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapQuoteUMConverter
import com.tangem.features.swap.v2.impl.amount.model.transformers.*
import com.tangem.features.swap.v2.impl.chooseprovider.SwapChooseProviderComponent
import com.tangem.features.swap.v2.impl.common.SwapAlertFactory
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import kotlin.properties.Delegates
import com.tangem.utils.transformer.update as transformerUpdate

@Suppress("LargeClass", "LongParameterList")
@ModelScoped
internal class SwapAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val selectInitialPairUseCase: SelectInitialPairUseCase,
    private val getSwapQuoteUseCase: GetSwapQuoteUseCase,
    private val swapChooseTokenNetworkListener: SwapChooseTokenNetworkListener,
    private val getAllowanceUseCase: GetAllowanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserCountryUseCase: GetUserCountryUseCase,
    private val appRouter: AppRouter,
    private val swapAmountAlertFactory: SwapAmountAlertFactory,
    private val swapAlertFactory: SwapAlertFactory,
    private val swapAmountUpdateListener: SwapAmountUpdateListener,
    private val swapAmountReduceListener: SwapAmountReduceListener,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
) : Model(), SwapAmountClickIntents, SwapChooseProviderComponent.ModelCallback {

    private val params: SwapAmountComponentParams = paramsContainer.require()
    private val swapDirection = params.swapDirection
    private var appCurrency = AppCurrency.Default
    private var userWallet = params.userWallet

    private var primaryCryptoCurrency: CryptoCurrency = params.primaryCryptoCurrencyStatusFlow.value.currency

    private var primaryMaximumAmountBoundary: EnterAmountBoundary by Delegates.notNull()
    private var primaryMinimumAmountBoundary: EnterAmountBoundary by Delegates.notNull()
    private var secondaryMaximumAmountBoundary: EnterAmountBoundary? = null
    private var secondaryMinimumAmountBoundary: EnterAmountBoundary? = null

    var userCountry: UserCountry = UserCountry.Other(Locale.getDefault().country)
    val bottomSheetNavigation: SlotNavigation<SwapChooseProviderConfig> = SlotNavigation()

    val uiState: StateFlow<SwapAmountUM>
    field = MutableStateFlow(params.amountUM)

    private val amountDebouncer = Debouncer()
    private val quoteTaskScheduler = SingleTaskScheduler<Unit>()

    init {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            userCountry = getUserCountryUseCase.invokeSync().getOrNull()
                ?: UserCountry.Other(Locale.getDefault().country)
        }
        configAmountNavigation()
        subscribeOnCryptoCurrencyStatusFlow()
        subscribeOnAmountUpdateTriggerUpdates()
        observeChooseSelectToken()
        subscribeOnAmountReduceToTriggerUpdates()
        subscribeOnAmountReduceByTriggerUpdates()
        subscribeOnAmountIgnoreReduceTriggerUpdates()
        subscribeOnReloadQuotesTriggerUpdates()
    }

    fun onStart() {
        startLoadingQuotesTask(isSilentReload = false)
    }

    fun onStop() {
        quoteTaskScheduler.cancelTask()
    }

    override fun onDestroy() {
        quoteTaskScheduler.cancelTask()
        super.onDestroy()
    }

    fun updateState(amountUM: SwapAmountUM) {
        uiState.update { amountUM }
    }

    override fun onProviderResult(quoteUM: SwapQuoteUM) {
        uiState.transformerUpdate(
            SwapAmountSelectQuoteTransformer(
                quoteUM = quoteUM,
                secondaryMaximumAmountBoundary = secondaryMaximumAmountBoundary,
                secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
            ),
        )
    }

    override fun onExpandEditField(selectedAmountType: SwapAmountType) {
        uiState.update { amountUM ->
            if (amountUM !is SwapAmountUM.Content) return
            amountUM.copy(
                selectedAmountType = selectedAmountType,
            )
        }
    }

    override fun onInfoClick() {
        val amountUM = uiState.value as? SwapAmountUM.Content ?: return
        val selectedProvider = amountUM.selectedQuote.provider ?: return

        swapAmountAlertFactory.priceImpactAlert(
            hasPriceImpact = (amountUM.secondaryAmount as? SwapAmountFieldUM.Content)?.priceImpact != null,
            currencySymbol = amountUM.primaryCryptoCurrencyStatus.currency.symbol,
            provider = selectedProvider,
        )
    }

    override fun onAmountValueChange(value: String) {
        uiState.transformerUpdate(
            SwapAmountValueChangeTransformer(
                primaryMaximumAmountBoundary = primaryMaximumAmountBoundary,
                secondaryMaximumAmountBoundary = secondaryMaximumAmountBoundary,
                primaryMinimumAmountBoundary = primaryMinimumAmountBoundary,
                secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
                value = value,
            ),
        )
        amountDebouncer.debounce(
            coroutineScope = modelScope,
            waitMs = DEBOUNCE_AMOUNT_DELAY,
            forceUpdate = true,
            destinationFunction = {
                startLoadingQuotesTask(isSilentReload = false)
            },
        )
    }

    override fun onAmountPasteTriggerDismiss() {
        uiState.transformerUpdate(SwapAmountPasteTransformer)
    }

    override fun onMaxValueClick() {
        uiState.transformerUpdate(
            SwapAmountValueMaxTransformer(
                primaryMaximumAmountBoundary = primaryMaximumAmountBoundary,
                secondaryMaximumAmountBoundary = secondaryMaximumAmountBoundary,
                primaryMinimumAmountBoundary = primaryMinimumAmountBoundary,
                secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
            ),
        )
        startLoadingQuotesTask(isSilentReload = false)
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        uiState.transformerUpdate(
            SwapAmountChangeCurrencyTransformer(
                isFiatSelected = isFiat,
            ),
        )
    }

    override fun onAmountNext() {
        saveResult()
    }

    override fun onSelectTokenClick() {
        val amountParams = params as? SwapAmountComponentParams.AmountParams ?: return
        modelScope.launch {
            val isEditMode = amountParams.currentRoute.firstOrNull()?.isEditMode == true
            val selectedCurrency = (uiState.value as? SwapAmountUM.Content)?.secondaryCryptoCurrencyStatus?.currency
            appRouter.push(
                AppRoute.ChooseManagedTokens(
                    userWalletId = userWallet.walletId,
                    initialCurrency = primaryCryptoCurrency,
                    selectedCurrency = selectedCurrency.takeIf { isEditMode },
                    source = AppRoute.ChooseManagedTokens.Source.SendViaSwap,
                ),
            )
        }
    }

    override fun onSeparatorClick() {
        val amountParams = params as? SwapAmountComponentParams.AmountParams ?: return

        modelScope.launch {
            if (amountParams.currentRoute.firstOrNull()?.isEditMode == true) {
                swapAmountAlertFactory.showCloseSendWithSwapAlert {
                    params.callback.resetSendWithSwapNavigation(resetNavigation = true)
                    confirmSendWithSwapClose()
                }
            } else {
                confirmSendWithSwapClose()
                amountParams.callback.onAmountResult(uiState.value)
            }
        }
    }

    private fun confirmSendWithSwapClose() {
        val amountParams = params as? SwapAmountComponentParams.AmountParams ?: return
        val amountFieldData = uiState.value.primaryAmount.amountField as? AmountState.Data

        val primaryCryptoCurrencyStatus = (uiState.value as? SwapAmountUM.Content)?.primaryCryptoCurrencyStatus
        if (primaryCryptoCurrencyStatus != null) {
            uiState.transformerUpdate(
                SwapAmountPrimaryReadyStateTransformer(
                    userWallet = userWallet,
                    primaryCryptoCurrencyStatus = primaryCryptoCurrencyStatus,
                    appCurrency = appCurrency,
                    swapDirection = swapDirection,
                    clickIntents = this,
                    isBalanceHidden = params.isBalanceHidingFlow.value,
                ),
            )
        }

        amountParams.callback.onSeparatorClick(lastAmount = amountFieldData?.amountTextField?.value.orEmpty())
    }

    private fun subscribeOnCryptoCurrencyStatusFlow() {
        params.primaryCryptoCurrencyStatusFlow
            .distinctUntilChanged { old, new -> old.value.amount == new.value.amount } // Check only balance changes
            .onEach { primaryCurrencyStatus ->
                val secondaryStatus = (uiState.value as? SwapAmountUM.Content)?.secondaryCryptoCurrencyStatus
                initCurrencies(
                    primaryStatus = primaryCurrencyStatus,
                    secondaryStatus = secondaryStatus,
                )
                if (secondaryStatus != null) {
                    uiState.transformerUpdate(
                        SwapAmountUpdateBalanceTransformer(
                            cryptoCurrencyStatus = primaryCurrencyStatus,
                            primaryMaximumAmountBoundary = primaryMaximumAmountBoundary,
                            primaryMinimumAmountBoundary = primaryMinimumAmountBoundary,
                        ),
                    )
                } else {
                    uiState.transformerUpdate(
                        SwapAmountPrimaryReadyStateTransformer(
                            userWallet = userWallet,
                            primaryCryptoCurrencyStatus = primaryCurrencyStatus,
                            appCurrency = appCurrency,
                            swapDirection = swapDirection,
                            clickIntents = this,
                            isBalanceHidden = params.isBalanceHidingFlow.value,
                        ),
                    )
                }
            }.launchIn(modelScope)
    }

    private fun subscribeOnAmountUpdateTriggerUpdates() {
        swapAmountUpdateListener.updateAmountTriggerFlow
            .onEach {
                if (uiState.value is SwapAmountUM.Content) {
                    onAmountValueChange(it)
                    saveResult()
                }
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountReduceToTriggerUpdates() {
        swapAmountReduceListener.reduceToTriggerFlow
            .onEach { reduceTo ->
                uiState.update(
                    SwapAmountReduceToTransformer(
                        primaryMinimumAmountBoundary = primaryMinimumAmountBoundary,
                        secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
                        reduceToValue = reduceTo,
                    ),
                )
                feeSelectorReloadTrigger.triggerLoadingState()
                startLoadingQuotesTask(isSilentReload = false)
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountReduceByTriggerUpdates() {
        swapAmountReduceListener.reduceByTriggerFlow
            .onEach { reduceBy ->
                uiState.update(
                    SwapAmountReduceByTransformer(
                        primaryMinimumAmountBoundary = primaryMinimumAmountBoundary,
                        secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
                        reduceByData = reduceBy,
                    ),
                )
                feeSelectorReloadTrigger.triggerLoadingState()
                startLoadingQuotesTask(isSilentReload = false)
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountIgnoreReduceTriggerUpdates() {
        swapAmountReduceListener.ignoreReduceTriggerFlow
            .onEach { uiState.update(SwapAmountIgnoreReduceTransformer) }
            .launchIn(modelScope)
    }

    private fun subscribeOnReloadQuotesTriggerUpdates() {
        swapAmountUpdateListener.reloadQuotesTriggerFlow
            .onEach { startLoadingQuotesTask(isSilentReload = true) }
            .launchIn(modelScope)
    }

    private fun observeChooseSelectToken() {
        swapChooseTokenNetworkListener.swapChooseTokenNetworkResultFlow
            .onEach { data ->
                (params as? SwapAmountComponentParams.AmountParams)?.callback?.resetSendWithSwapNavigation(
                    data.shouldResetNavigation,
                )
                uiState.update { amountUM ->
                    if (amountUM is SwapAmountUM.Content) {
                        initPairs(data.swapCurrencies, data.cryptoCurrency)
                        amountUM.copy(
                            isPrimaryButtonEnabled = false,
                            secondaryAmount = SwapAmountFieldUM.Loading(
                                amountType = SwapAmountType.To,
                            ),
                        )
                    } else {
                        amountUM
                    }
                }
            }
            .launchIn(modelScope)
    }

    private fun initPairs(swapCurrencies: SwapCurrencies, secondaryCryptoCurrency: CryptoCurrency?) {
        modelScope.launch {
            val secondaryStatus = selectInitialPairUseCase(
                primaryCryptoCurrency = primaryCryptoCurrency,
                secondaryCryptoCurrency = secondaryCryptoCurrency,
                userWallet = userWallet,
                swapCurrencies = swapCurrencies,
                swapDirection = params.swapDirection,
            )

            val primaryStatus = (uiState.value as? SwapAmountUM.Content)?.primaryCryptoCurrencyStatus
            if (secondaryStatus != null && primaryStatus != null) {
                initCurrencies(primaryStatus, secondaryStatus)
                uiState.transformerUpdate(
                    SwapAmountSecondaryReadyStateTransformer(
                        userWallet = userWallet,
                        swapCurrencies = swapCurrencies,
                        primaryCryptoCurrencyStatus = primaryStatus,
                        secondaryCryptoCurrencyStatus = secondaryStatus,
                        appCurrency = appCurrency,
                        swapDirection = swapDirection,
                        clickIntents = this@SwapAmountModel,
                        isBalanceHidden = params.isBalanceHidingFlow.value,
                    ),
                )
                startLoadingQuotesTask(isSilentReload = false)
            } else {
                Timber.e(
                    """
                        Invalid cryptocurrencies status: 
                        | Primary -> $primaryStatus
                        | Secondary -> $secondaryStatus
                    """.trimIndent(),
                )
                showErrorAlert(errorMessage = null)
            }
        }
    }

    private suspend fun initCurrencies(primaryStatus: CryptoCurrencyStatus, secondaryStatus: CryptoCurrencyStatus?) {
        primaryMinimumAmountBoundary = EnterAmountBoundary(
            amount = getMinimumTransactionAmountSyncUseCase
                .invoke(
                    userWalletId = userWallet.walletId,
                    cryptoCurrencyStatus = primaryStatus,
                ).getOrNull().orZero(),
            fiatRate = primaryStatus.value.fiatRate,
            fiatAmount = primaryStatus.value.fiatAmount,
        )
        primaryMaximumAmountBoundary = MaxEnterAmountConverter().convert(primaryStatus)

        if (secondaryStatus != null) {
            secondaryMinimumAmountBoundary = EnterAmountBoundary(
                amount = getMinimumTransactionAmountSyncUseCase
                    .invoke(
                        userWalletId = userWallet.walletId,
                        cryptoCurrencyStatus = secondaryStatus,
                    ).getOrNull().orZero(),
                fiatRate = secondaryStatus.value.fiatRate,
                fiatAmount = secondaryStatus.value.fiatAmount,
            )
            secondaryMaximumAmountBoundary = MaxEnterAmountConverter().convert(secondaryStatus)
        }
    }

    private fun loadQuotes(isSilentReload: Boolean) {
        val state = uiState.value as? SwapAmountUM.Content ?: return
        if (state.secondaryCryptoCurrencyStatus == null) return

        val (fromCryptoCurrency, toCryptoCurrency) = state.swapDirection.withSwapDirection(
            onDirect = { state.primaryCryptoCurrencyStatus.currency to state.secondaryCryptoCurrencyStatus.currency },
            onReverse = { state.secondaryCryptoCurrencyStatus.currency to state.primaryCryptoCurrencyStatus.currency },
        )

        val fromAmount = when (state.swapDirection) {
            SwapDirection.Direct -> state.primaryAmount.amountField
            SwapDirection.Reverse -> state.secondaryAmount.amountField
        } as? AmountState.Data

        if (fromAmount?.amountTextField?.isError == true) return

        val fromAmountValue = fromAmount?.amountTextField?.cryptoAmount?.value ?: return

        val swapGroups = state.swapCurrencies.getGroupWithDirection(state.swapDirection)

        uiState.transformerUpdate(SwapQuoteLoadingStateTransformer)

        modelScope.launch {
            val quotes = swapGroups.available.filter {
                it.currencyStatus.currency.id == toCryptoCurrency.id
            }.flatMap {
                it.providers
            }.map { provider ->
                async {
                    getSwapQuoteUseCase(
                        userWallet = userWallet,
                        fromCryptoCurrency = fromCryptoCurrency,
                        toCryptoCurrency = toCryptoCurrency,
                        fromAmount = fromAmountValue,
                        provider = provider,
                    ).fold(
                        ifLeft = { error ->
                            SwapQuoteUM.Error(
                                provider = provider,
                                expressError = error,
                            )
                        },
                        ifRight = { quote: SwapQuoteModel ->
                            SwapQuoteUMConverter(
                                primaryCurrency = fromCryptoCurrency,
                                secondaryCurrency = toCryptoCurrency,
                                swapDirection = swapDirection,
                                allowanceContract = quote.allowanceContract,
                                isApprovalNeeded = checkAllowance(state, quote),
                                fromAmount = fromAmountValue,
                            ).convert(
                                SwapQuoteUMConverter.Data(
                                    quote = quote,
                                    provider = provider,
                                ),
                            )
                        },
                    )
                }
            }.awaitAll()

            uiState.transformerUpdate(
                SwapAmountSetQuotesTransformer(
                    quotes = quotes,
                    secondaryMaximumAmountBoundary = secondaryMaximumAmountBoundary,
                    secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
                    isSilentReload = isSilentReload,
                ),
            )

            feeSelectorReloadTrigger.triggerUpdate()
        }
    }

    private fun startLoadingQuotesTask(isSilentReload: Boolean) {
        quoteTaskScheduler.cancelTask()
        loadQuotes(isSilentReload = isSilentReload)
        quoteTaskScheduler.scheduleTask(
            scope = modelScope,
            task = loadQuotesTask(),
        )
    }

    private fun loadQuotesTask(): PeriodicTask<Unit> {
        return PeriodicTask(
            delay = QUOTES_UPDATE_DELAY,
            isDelayFirst = true,
            task = {
                runCatching { loadQuotes(isSilentReload = true) }
            },
            onSuccess = { /* no-op */ },
            onError = { /* no-op */ },
        )
    }

    private suspend fun checkAllowance(state: SwapAmountUM.Content, quote: SwapQuoteModel): Boolean {
        val allowanceContract = quote.allowanceContract
        val allowance = if (allowanceContract != null) {
            getAllowanceUseCase(
                userWalletId = userWallet.walletId,
                cryptoCurrency = state.primaryCryptoCurrencyStatus.currency,
                spenderAddress = allowanceContract,
            ).getOrNull()
        } else {
            BigDecimal.ZERO
        }

        return allowance.orZero() < state.primaryCryptoCurrencyStatus.value.amount.orZero()
    }

    private fun saveResult() {
        val params = params as? SwapAmountComponentParams.AmountParams ?: return
        params.callback.onAmountResult(uiState.value)
    }

    private fun showErrorAlert(errorMessage: String?) {
        swapAlertFactory.getGenericErrorState(
            expressError = ExpressError.UnknownError,
            onFailedTxEmailClick = {
                modelScope.launch {
                    swapAlertFactory.onFailedTxEmailClick(
                        userWallet = params.userWallet,
                        cryptoCurrency = primaryCryptoCurrency,
                        errorMessage = errorMessage,
                    )
                }
            },
            popBack = appRouter::pop,
        )
    }

    private fun configAmountNavigation() {
        val params = params as? SwapAmountComponentParams.AmountParams ?: return
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).onEach { (state, route) ->
            params.callback.onNavigationResult(
                NavigationUM.Content(
                    title = resourceReference(R.string.common_swap),
                    subtitle = null,
                    backIconRes = if (route.isEditMode) {
                        R.drawable.ic_back_24
                    } else {
                        R.drawable.ic_close_24
                    },
                    backIconClick = params.callback::onBackClick,
                    primaryButton = NavigationButton(
                        textReference = if (route.isEditMode) {
                            resourceReference(R.string.common_continue)
                        } else {
                            resourceReference(R.string.common_next)
                        },
                        isEnabled = state.isPrimaryButtonEnabled,
                        onClick = {
                            saveResult()
                            params.callback.onNextClick()
                        },
                    ),
                ),
            )
        }.launchIn(modelScope)
    }

    private companion object {
        const val DEBOUNCE_AMOUNT_DELAY = 1000L
        const val QUOTES_UPDATE_DELAY = 10000L
    }
}