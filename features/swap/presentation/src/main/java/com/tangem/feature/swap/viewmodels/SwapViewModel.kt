package com.tangem.feature.swap.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.models.ApprovePermissionButton
import com.tangem.feature.swap.models.CancelPermissionButton
import com.tangem.feature.swap.models.FeeState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardData
import com.tangem.feature.swap.models.SwapPermissionStateHolder
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.SwapSuccessStateHolder
import com.tangem.feature.swap.models.SwapWarning
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelect
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.router.SwapScreen
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class SwapViewModel @Inject constructor(
    private val referralInteractor: SwapInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    var uiState: SwapStateHolder by mutableStateOf(createInitialUiState())
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

    private fun createInitialUiState(): SwapStateHolder { // TODO: create using real state
        return createTestUiState()
    }

    // region temp for test
    init {
        viewModelScope.launch {
            runCatching {
                val tokens = referralInteractor.getTokensToSwap("binance-smart-chain")
                val token = tokens[1]
                val token2 = tokens[2]
            }.onFailure {
                Log.e("SwapViewModel", it.message ?: it.cause.toString())
            }
        }
    }

    private fun createTestUiState(): SwapStateHolder {
        val sendCard = SwapCardData(
            type = TransactionCardType.SendCard("123", false),
            amount = "1 000 000 000 000 000 000",
            amountEquivalent = "1 000 000 000 000 000 000",
            tokenIconUrl = "",
            tokenCurrency = "DAI",
            networkIconRes = R.drawable.img_polygon_22,
        )

        val receiveCard = SwapCardData(
            type = TransactionCardType.ReceiveCard(),
            amount = "1 000 000",
            amountEquivalent = "1 000 000",
            tokenIconUrl = "",
            tokenCurrency = "DAI",
            networkIconRes = R.drawable.img_polygon_22,
            canSelectAnotherToken = true,
        )

        val permissionState = SwapPermissionStateHolder(
            currency = "DAI",
            amount = "∞",
            walletAddress = "",
            spenderAddress = "",
            fee = "2,14$",
            approveButton = ApprovePermissionButton(true) {},
            cancelButton = CancelPermissionButton(true) {},
        )

        val token = TokenToSelect(
            id = "USDC",
            name = "USDC",
            symbol = "USDC",
            iconUrl = "",
            addedTokenBalanceData = TokenBalanceData(amount = "15 000 $", amountEquivalent = "15 000 USDT"),
        )

        val selectTokenState = SwapSelectTokenStateHolder(
            listOf(token, token, token, token, token, token, token, token, token),
            listOf(token, token, token, token, token, token, token, token, token), {}, {},
        )

        return SwapStateHolder(
            sendCardData = sendCard,
            receiveCardData = receiveCard,
            fee = FeeState.Loaded(fee = "0.155 MATIC (0.14 $)"),
            warnings = listOf(SwapWarning.PermissionNeeded("DAI")),
            networkCurrency = "MATIC",
            swapButton = SwapButton(enabled = true, loading = false, onClick = {}),
            onRefresh = {}, onBackClicked = {}, onChangeCardsClicked = {},
            permissionState = permissionState,
            successState = SwapSuccessStateHolder("Success"),
            selectTokenState = selectTokenState,
        )
    }
    // endregion temp for test
}
