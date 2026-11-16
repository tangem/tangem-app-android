package com.tangem.features.earn.impl.getfunds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext

internal class EarnGetTokenSheet(
    private val context: AppComponentContext,
    private val sheetFactory: EarnGetTokenSheetComponent.Factory,
) {

    private val navigation = SlotNavigation<EarnGetTokenSheetRequest>()

    private val slot = context.childSlot(
        key = "getTokenSheet",
        source = navigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { request, factoryContext ->
            sheetFactory.create(
                context = context.childByContext(componentContext = factoryContext),
                params = EarnGetTokenSheetComponent.Params(request = request, onDismiss = navigation::dismiss),
            )
        },
    )

    fun show(request: EarnGetTokenSheetRequest) {
        navigation.activate(request)
    }

    @Composable
    fun BottomSheet() {
        val slotState by slot.subscribeAsState()
        slotState.child?.instance?.BottomSheet()
    }
}