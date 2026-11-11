package com.tangem.features.yield.supply.impl.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorComponent
import com.tangem.features.send.api.subcomponents.feeSelector.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams

/** Shows the fee token list over a Yield Mode action sheet and hands the picked currency back to its model. */
internal class YieldSupplyFeeCurrencySelector(
    private val context: AppComponentContext,
    private val feeSelectorComponentFactory: FeeSelectorComponent.Factory,
    private val createParams: (FeeSelectorModelCallback) -> FeeSelectorParams.FeeSelectorDetailsParams?,
    private val onPicked: (CryptoCurrencyStatus) -> Unit,
) : FeeSelectorModelCallback {

    private val navigation = SlotNavigation<Unit>()

    private val slot = context.childSlot(
        key = "yieldSupplyFeeCurrencySelector",
        source = navigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { _, factoryContext ->
            feeSelectorComponentFactory.create(
                context = context.childByContext(componentContext = factoryContext),
                params = requireNotNull(createParams(this)) { "The fee token list opens only over a loaded fee" },
                onDismiss = navigation::dismiss,
            )
        },
    )

    fun show() {
        if (createParams(this) == null) return
        // The sheet renders nothing while its list fails to load yet stays active, so a re-tap starts it over
        navigation.dismiss()
        navigation.activate(Unit)
    }

    override fun onFeeResult(feeSelectorUM: FeeSelectorUM) {
        navigation.dismiss()
        val picked = (feeSelectorUM as? FeeSelectorUM.Content)?.feeExtraInfo?.feeCryptoCurrencyStatus ?: return
        onPicked(picked)
    }

    @Composable
    fun BottomSheet() {
        val slotState by slot.subscribeAsState()
        slotState.child?.instance?.BottomSheet()
    }
}