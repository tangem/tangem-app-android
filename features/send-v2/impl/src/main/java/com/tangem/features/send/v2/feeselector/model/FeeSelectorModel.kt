package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.utils.stack
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.route.FeeSelectorRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.isSingleItem
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    feeSelectorLogicFactory: FeeSelectorLogic.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), FeeSelectorIntents {

    private val params = paramsContainer.require<FeeSelectorParams.FeeSelectorDetailsParams>()
    private val feeSelectorLogic = feeSelectorLogicFactory.create(
        params = params,
        modelScope = modelScope,
    )

    val stackNavigation = StackNavigation<FeeSelectorRoute>()
    val uiState = feeSelectorLogic.uiState

    override fun onFeeItemSelected(feeItem: FeeItem) {
        feeSelectorLogic.onFeeItemSelected(feeItem)

        if (contentState().selectedFeeItem is FeeItem.Custom) {
            return
        }

        modelScope.launch {
            val stack = stackNavigation.stack()
            if (stack.isSingleItem() && stack[0] is FeeSelectorRoute.ChooseSpeed) {
                // In case of only speed selection, we finish the flow immediately after selection
                feeSelectorLogic.onDoneClick()
                params.callback.onFeeResult(uiState.value)
            } else {
                stackNavigation.pop()
            }
        }
    }

    override fun onTokenSelected(status: CryptoCurrencyStatus) {
        feeSelectorLogic.onTokenSelected(status)
        stackNavigation.pop()
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        feeSelectorLogic.onCustomFeeValueChange(index, value)
    }

    override fun onNonceChange(value: String) {
        feeSelectorLogic.onNonceChange(value)
    }

    override fun onDoneClick() {
        modelScope.launch {
            val stack = stackNavigation.stack()
            if (stack.isSingleItem()) {
                feeSelectorLogic.onDoneClick()
                params.callback.onFeeResult(uiState.value)
            } else {
                stackNavigation.pop()
            }
        }
    }

    fun onFeeScreenOpened() {
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.FeeSummaryScreenOpened(
                categoryName = params.analyticsCategoryName,
                source = params.analyticsSendSource,
                blockchain = params.cryptoCurrencyStatus.currency.network.name,
                token = params.cryptoCurrencyStatus.currency.symbol,
            ),
        )
    }

    fun onChooseSpeedScreenOpened() {
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.FeeScreenOpened(
                categoryName = params.analyticsCategoryName,
                source = params.analyticsSendSource,
                blockchain = params.cryptoCurrencyStatus.currency.network.name,
                token = params.cryptoCurrencyStatus.currency.symbol,
            ),
        )
    }

    fun onChooseTokenScreenOpened() {
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.FeeTokenScreenOpened(
                categoryName = params.analyticsCategoryName,
                source = params.analyticsSendSource,
                availableTokens = getAvailableTokensString(),
                blockchain = params.cryptoCurrencyStatus.currency.network.name,
            ),
        )
    }

    private fun getAvailableTokensString(): String {
        return (uiState.value as? FeeSelectorUM.Content)
            ?.feeExtraInfo
            ?.availableFeeCurrencies
            ?.joinToString(", ") { it.currency.symbol }
            .orEmpty()
    }

    private fun contentState(): FeeSelectorUM.Content {
        val currentState = uiState.value
        require(currentState is FeeSelectorUM.Content) { "Current state must be FeeSelectorUM.Content" }
        return currentState
    }

    fun getInitialRoute(): FeeSelectorRoute {
        return when {
            feeSelectorLogic.isGaslessEnabled -> FeeSelectorRoute.NetworkFee
            else -> FeeSelectorRoute.ChooseSpeed
        }
    }
}