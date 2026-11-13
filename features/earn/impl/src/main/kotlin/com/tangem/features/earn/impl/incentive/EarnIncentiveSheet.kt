package com.tangem.features.earn.impl.incentive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext

internal class EarnIncentiveSheet(
    private val context: AppComponentContext,
    private val sheetFactory: EarnIncentiveSheetComponent.Factory,
) : EarnIncentiveSheetComponent.ModelCallback {

    private val navigation = SlotNavigation<EarnIncentiveSheetRequest>()

    private val slot = context.childSlot(
        key = "incentiveSheet",
        source = navigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { request, factoryContext ->
            sheetFactory.create(
                context = context.childByContext(componentContext = factoryContext),
                params = EarnIncentiveSheetComponent.Params(request = request, callback = this),
            )
        },
    )

    fun show(request: EarnIncentiveSheetRequest) {
        navigation.activate(request)
    }

    override fun onDismiss() {
        navigation.dismiss()
    }

    @Composable
    fun BottomSheet() {
        val slotState by slot.subscribeAsState()
        slotState.child?.instance?.BottomSheet()
    }
}