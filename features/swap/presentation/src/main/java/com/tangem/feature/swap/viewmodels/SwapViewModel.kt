package com.tangem.feature.swap.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TxState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.router.SwapScreen
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.toFormattedString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class SwapViewModel @Inject constructor(
    private val swapInteractor: SwapInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val currency = Json.decodeFromString<Currency>(
        savedStateHandle[SwapFragment.CURRENCY_BUNDLE_KEY]
            ?: error("no expected parameter Currency found"),
    )

    private val stateBuilder = StateBuilder(
        actions = createUiActions(),
    )
    private val amountDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler<SwapState>()

    private var dataState by mutableStateOf(SwapProcessDataState(networkId = currency.networkId))
    var uiState: SwapStateHolder by mutableStateOf(stateBuilder.createInitialLoadingState(currency))
        private set

    // shows currency order (direct - swap initial to selected, reversed = selected to initial)
    private var isOrderReversed = false
    private val lastAmount = mutableStateOf(INITIAL_AMOUNT)
    private var swapRouter: SwapRouter by Delegates.notNull()
    var currentScreen = SwapScreen.Main
        get() = swapRouter.currentScreen

    init {
        initTokens(currency)
    }

    override fun onCleared() {
        singleTaskScheduler.cancelTask()
        super.onCleared()
    }

    fun setRouter(router: SwapRouter) {
        swapRouter = router
        uiState = uiState.copy(
            onBackClicked = router::back,
            onSelectTokenClick = { router.openScreen(SwapScreen.SelectToken) },
            onSuccess = { router.openScreen(SwapScreen.Success) },
        )
    }

    private fun initTokens(currency: Currency) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.initTokensToSwap(currency)
            }
                .onSuccess { state ->
                    dataState = dataState.copy(
                        fromCurrency = state.preselectTokens.fromToken,
                        toCurrency = state.preselectTokens.toToken,
                    )
                    updateTokensState(dataState = state.foundTokensState)
                    startLoadingQuotes(
                        fromToken = state.preselectTokens.fromToken,
                        toToken = state.preselectTokens.toToken,
                        amount = lastAmount.value,
                    )
                }
                .onFailure {
                    Log.e("SwapViewModel", it.message ?: it.cause.toString())
                }
        }
    }

    private fun updateTokensState(dataState: FoundTokensState) {
        uiState = stateBuilder.addTokensToState(uiState, dataState)
    }

    private fun startLoadingQuotes(fromToken: Currency, toToken: Currency, amount: String) {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createQuotesLoadingState(uiState, fromToken, toToken, currency.id)
        singleTaskScheduler.scheduleTask(
            viewModelScope,
            loadQuotesTask(
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
            ),
        )
    }

    private fun loadQuotesTask(
        fromToken: Currency,
        toToken: Currency,
        amount: String,
    ): PeriodicTask<SwapState> {
        return PeriodicTask(
            UPDATE_DELAY,
            task = {
                uiState = stateBuilder.createSilentLoadState(uiState)
                runCatching(dispatchers.io) {
                    dataState = dataState.copy(
                        amount = amount,
                        swapModel = null,
                        estimatedGas = null,
                        approveModel = null,
                    )
                    swapInteractor.findBestQuote(
                        networkId = dataState.networkId,
                        fromToken = fromToken,
                        toToken = toToken,
                        amountToSwap = amount,
                    )
                }
            },
            onSuccess = { swapState ->
                when (swapState) {
                    is SwapState.QuotesLoadedState -> {
                        fillDataState(swapState.permissionState, swapState.swapDataModel)
                        uiState = stateBuilder.createQuotesLoadedState(
                            uiStateHolder = uiState,
                            quoteModel = swapState,
                            fromToken = fromToken,
                        )
                    }
                    is SwapState.EmptyAmountState -> {
                        uiState = stateBuilder.createQuotesEmptyAmountState(
                            uiStateHolder = uiState,
                            emptyAmountState = swapState,
                        )
                    }
                    is SwapState.SwapError -> {
                        uiState = stateBuilder.mapError(uiState, swapState.error)
                    }
                }
            },
            onError = {
                uiState = stateBuilder.addWarning(uiState, it.message)
            },
        )
    }

    private fun fillDataState(permissionState: PermissionDataState, swapDataModel: SwapDataModel?) {
        dataState = if (permissionState is PermissionDataState.PermissionReadyForRequest) {
            dataState.copy(
                estimatedGas = permissionState.requestApproveData.estimatedGas,
                approveModel = permissionState.requestApproveData.approveModel,
            )
        } else {
            dataState.copy(
                swapModel = swapDataModel,
            )
        }
    }

    private fun onSwapClick() {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createSwapInProgressState(uiState)
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.onSwap(
                    networkId = dataState.networkId,
                    swapData = dataState.swapModel!!,
                    currencyToSend = dataState.fromCurrency!!,
                    currencyToGet = dataState.toCurrency!!,
                    amountToSwap = dataState.amount!!,
                )
            }
                .onSuccess {
                    when (it) {
                        is TxState.TxSent -> {
                            uiState = stateBuilder.createSuccessState(uiState, it)
                            swapRouter.openScreen(SwapScreen.Success)
                        }
                        else -> {
                            uiState = stateBuilder.createSwapErrorTransaction(uiState) {
                                uiState = stateBuilder.clearAlert(uiState)
                            }
                        }
                    }
                }
                .onFailure { }
        }
    }

    private fun givePermissionsToSwap() {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.givePermissionToSwap(
                    networkId = dataState.networkId,
                    estimatedGas = dataState.estimatedGas!!,
                    transactionData = dataState.approveModel!!,
                    forTokenContractAddress = (dataState.fromCurrency as? Currency.NonNativeToken)?.contractAddress
                        ?: "",
                )
            }
                .onSuccess {
                    when (it) {
                        is TxState.TxSent -> {
                            uiState = stateBuilder.loadingPermissionState(uiState)
                        }
                        else -> {
                            uiState = stateBuilder.addAlert(uiState) {
                                uiState = stateBuilder.clearAlert(uiState)
                            }
                        }
                    }
                }
                .onFailure {
                    uiState = stateBuilder.addAlert(uiState) {
                        uiState = stateBuilder.clearAlert(uiState)
                    }
                }
        }
    }

    private fun onSearchEntered(searchQuery: String) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.searchTokens(dataState.networkId, searchQuery)
            }
                .onSuccess {
                    updateTokensState(it)
                }
                .onFailure { }
        }
    }

    private fun onTokenSelect(id: String) {
        val foundToken = swapInteractor.findTokenById(id)
        if (foundToken != null) {
            val fromToken: Currency
            val toToken: Currency
            if (isOrderReversed) {
                fromToken = foundToken
                toToken = currency
            } else {
                fromToken = currency
                toToken = foundToken
            }
            dataState = dataState.copy(
                fromCurrency = fromToken,
                toCurrency = toToken,
            )
            startLoadingQuotes(fromToken, toToken, lastAmount.value)
            swapRouter.openScreen(SwapScreen.Main)
        }
    }

    private fun onChangeCardsClicked() {
        val newFromToken = dataState.toCurrency
        val newToToken = dataState.fromCurrency
        if (newFromToken != null && newToToken != null) {
            dataState = dataState.copy(
                fromCurrency = newFromToken,
                toCurrency = newToToken,
            )
            isOrderReversed = !isOrderReversed
            lastAmount.value = cutAmountWithDecimals(swapInteractor.getTokenDecimals(newFromToken), lastAmount.value)
            uiState = stateBuilder.updateSwapAmount(uiState, lastAmount.value)
            startLoadingQuotes(newFromToken, newToToken, lastAmount.value)
        }
    }

    private fun onAmountChanged(value: String) {
        val fromToken = dataState.fromCurrency
        val toToken = dataState.toCurrency
        if (fromToken != null && toToken != null) {
            val cutValue = cutAmountWithDecimals(swapInteractor.getTokenDecimals(fromToken), value)
            uiState = stateBuilder.updateSwapAmount(uiState, cutValue)
            lastAmount.value = cutValue
            amountDebouncer.debounce(DEBOUNCE_AMOUNT_DELAY, viewModelScope) {
                startLoadingQuotes(fromToken, toToken, lastAmount.value)
            }
        }
    }

    private fun onMaxAmountClicked() {
        dataState.fromCurrency?.let {
            val balance = swapInteractor.getTokenBalance(it)
            onAmountChanged(balance.formatToUIRepresentation())
        }
    }

    private fun cutAmountWithDecimals(maxDecimals: Int, amount: String): String {
        return amount.toBigDecimalOrNull()?.toFormattedString(maxDecimals) ?: INITIAL_AMOUNT
    }

    private fun createUiActions(): UiActions {
        return UiActions(
            onSearchEntered = { onSearchEntered(it) },
            onTokenSelected = { onTokenSelect(it) },
            onAmountChanged = { onAmountChanged(it) },
            onSwapClick = { onSwapClick() },
            onGivePermissionClick = { givePermissionsToSwap() },
            onChangeCardsClicked = { onChangeCardsClicked() },
            onBackClicked = { onSearchEntered("") },
            onMaxAmountSelected = { onMaxAmountClicked() },
        )
    }

    companion object {
        private const val INITIAL_AMOUNT = ""
        private const val UPDATE_DELAY = 10000L
        private const val DEBOUNCE_AMOUNT_DELAY = 1000L
    }
}