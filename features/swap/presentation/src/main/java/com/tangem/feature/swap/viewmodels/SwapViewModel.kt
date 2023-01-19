package com.tangem.feature.swap.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.createFromAmountWithoutOffset
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.router.SwapScreen
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.runCatching
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

    private val stateBuilder = StateBuilder(
        actions = createUiActions(),
    )

    private val amountDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler()
    private val currency = Json.decodeFromString<Currency>(
        savedStateHandle[SwapFragment.CURRENCY_BUNDLE_KEY]
            ?: error("no expected parameter Currency found"),
    )

    var uiState: SwapStateHolder by mutableStateOf(stateBuilder.createInitialLoadingState(currency.symbol))
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
            PeriodicTask(
                delay = UPDATE_DELAY,
            ) {
                loadQuotesInternal(
                    fromToken = fromToken,
                    toToken = toToken,
                    amount = amount,
                )
            },
        )
    }

    private fun loadQuotesInternal(fromToken: Currency, toToken: Currency, amount: String) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.findBestQuote(
                    fromToken = fromToken,
                    toToken = toToken,
                    amount = createFromAmountWithoutOffset(
                        amountWithoutOffset = amount,
                        decimals = swapInteractor.getTokenDecimals(fromToken),
                    ),
                )
            }
                .onSuccess { swapState ->
                    when (swapState) {
                        is SwapState.QuotesLoadedState -> {
                            uiState = stateBuilder.createQuotesLoadedState(
                                uiStateHolder = uiState,
                                quoteModel = swapState,
                                fromToken = fromToken,
                            )
                        }
                        is SwapState.SwapError -> {
                        }
                    }
                }
                .onFailure { }
        }
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
        )
    }

    private fun onSwapClick() {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createSwapInProgressState(uiState)
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.onSwap()
            }
                .onSuccess {
                    when (it) {
                        is SwapState.SwapError -> {
                        }
                        else -> {}
                    }
                }
                .onFailure { }
        }
    }

    private fun givePermissionsToSwap() {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.givePermissionToSwap()
            }
                .onSuccess { }
                .onFailure { }
        }
    }

    private fun onSearchEntered(searchQuery: String) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.onSearchToken(searchQuery)
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
            swapRouter.openScreen(SwapScreen.Main)
            startLoadingQuotes(fromToken, toToken, lastAmount.value)
        }
    }

    private fun onChangeCardsClicked() {
        val currencies = swapInteractor.getExchangeCurrencies()
        val newFromToken = currencies?.toCurrency
        val newToToken = currencies?.fromCurrency
        if (newFromToken != null && newToToken != null) {
            isOrderReversed = !isOrderReversed
            startLoadingQuotes(newFromToken, newToToken, lastAmount.value)
        }
    }

    private fun onAmountChanged(value: String) {
        uiState = stateBuilder.updateSwapAmount(uiState, value, value)
        lastAmount.value = value
        amountDebouncer.debounce(DEBOUNCE_AMOUNT_DELAY, viewModelScope) {
            val currencies = swapInteractor.getExchangeCurrencies()
            val fromToken = currencies?.fromCurrency
            val toToken = currencies?.toCurrency
            if (fromToken != null && toToken != null) {
                startLoadingQuotes(fromToken, toToken, lastAmount.value)
            }
        }
    }

    override fun onCleared() {
        singleTaskScheduler.cancelTask()
        super.onCleared()
    }

    companion object {
        private const val INITIAL_AMOUNT = "1"
        private const val UPDATE_DELAY = 10000L
        private const val DEBOUNCE_AMOUNT_DELAY = 1000L
    }
}
