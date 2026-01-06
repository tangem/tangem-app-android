package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.utils.stack
import com.tangem.features.send.v2.api.SendFeatureToggles
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
    private val sendFeatureToggles: SendFeatureToggles,
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

    private fun contentState(): FeeSelectorUM.Content {
        val currentState = uiState.value
        require(currentState is FeeSelectorUM.Content) { "Current state must be FeeSelectorUM.Content" }
        return currentState
    }

    fun getInitialRoute(): FeeSelectorRoute {
        if (sendFeatureToggles.isGaslessTransactionsEnabled.not()) {
            return FeeSelectorRoute.ChooseSpeed
        }

        // TODO: Add logic to choose initial route based on other conditions
        return FeeSelectorRoute.NetworkFee
    }
}