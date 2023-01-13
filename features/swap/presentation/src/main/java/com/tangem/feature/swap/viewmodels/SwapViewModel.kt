package com.tangem.feature.swap.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.Currency
import com.tangem.feature.swap.domain.models.SwapState
import com.tangem.feature.swap.domain.models.createFromAmountWithoutOffset
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.router.SwapScreen
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val stateBuilder = StateBuilder()
    private val currency = Json.decodeFromString<Currency>(
        savedStateHandle[SwapFragment.CURRENCY_BUNDLE_KEY]
            ?: error("no expected parameter Currency found"),
    )

    var uiState: SwapStateHolder by mutableStateOf(
        stateBuilder.createInitialLoadingState(currency.symbol)
        { /** [REDACTED_TODO_COMMENT]*/ },
    )
        private set

    private var swapRouter: SwapRouter by Delegates.notNull()

    var currentScreen = SwapScreen.Main
        get() = swapRouter.currentScreen

    fun setRouter(router: SwapRouter) {
        swapRouter = router
        uiState = uiState.copy(
            onBackClicked = router::back,
            onSelectTokenClick = { router.openScreen(SwapScreen.SelectToken) },
            onSuccess = { router.openScreen(SwapScreen.Success) },
        )
    }

    init {
        viewModelScope.launch(dispatchers.main) {
            runCatching {
                withContext(dispatchers.io) {
                    swapInteractor.getTokensToSwap(currency.networkId)
                }
            }
                .onSuccess { tokens ->
                    if (tokens.size > MIN_TOKENS_IN_LIST) {
                        tokens.firstOrNull { token ->
                            token.id == currency.id
                        }?.let {
                            loadQuotes(it, tokens[1], INITIAL_AMOUNT)
                        }
                    }
                }
                .onFailure {
                    Log.e("SwapViewModel", it.message ?: it.cause.toString())
                }
        }
    }

    private fun loadQuotes(fromToken: Currency, toToken: Currency, amount: String) {
        uiState = stateBuilder.createQuotesLoadingState(uiState, fromToken, toToken, currency.id)
        viewModelScope.launch(dispatchers.main) {
            runCatching {
                withContext(dispatchers.io) {
                    swapInteractor.findBestQuote(
                        fromToken = fromToken,
                        toToken = toToken,
                        amount = createFromAmountWithoutOffset(
                            amountWithoutOffset = amount,
                            decimals = swapInteractor.getTokenDecimals(fromToken),
                        ),
                    )
                }
            }
                .onSuccess { swapState ->
                    when (swapState) {
                        is SwapState.QuotesLoadedState -> {
                            uiState = stateBuilder.createQuotesLoadedState(
                                uiStateHolder = uiState,
                                quoteModel = swapState,
                                fromToken = fromToken,
                                onSwapClick = { onSwapClick(toToken, swapState) },
                                onGivePermissionClick = { givePermissionsToSwap(fromToken) },
                            )
                        }
                        is SwapState.SwapSuccess -> {
                            //todo implement
                        }
                        is SwapState.SwapError -> {
                        }
                    }
                }
                .onFailure { }
        }
    }

    private fun onSwapClick(toToken: Currency, quoteModel: SwapState.QuotesLoadedState) {
        viewModelScope.launch(dispatchers.main) {
            runCatching {
                withContext(dispatchers.io) {
                    if (!quoteModel.isAllowedToSpend) {
                        swapInteractor.givePermissionToSwap(toToken)
                    } else {
                        swapInteractor.onSwap()
                    }
                }
            }
                .onSuccess {
                    if (it is SwapState) {
                        when (it) {
                            is SwapState.SwapSuccess -> {
                            }
                            is SwapState.SwapError -> {
                            }
                            else -> {}
                        }
                    }
                }
                .onFailure { }
        }
    }

    private fun givePermissionsToSwap(tokenToApprove: Currency) {
        viewModelScope.launch(dispatchers.main) {
            runCatching {
                withContext(dispatchers.io) {
                    swapInteractor.givePermissionToSwap(tokenToApprove)
                }
            }
                .onSuccess { }
                .onFailure { }
        }
    }

    companion object {
        private const val MIN_TOKENS_IN_LIST = 2
        private const val INITIAL_AMOUNT = "1"
    }
}