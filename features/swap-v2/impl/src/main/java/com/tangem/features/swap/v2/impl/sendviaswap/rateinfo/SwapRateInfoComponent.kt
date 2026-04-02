package com.tangem.features.swap.v2.impl.sendviaswap.rateinfo

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.express.models.ExpressRateType

internal class SwapRateInfoComponent(
    appComponentContext: AppComponentContext,
    expressRateType: ExpressRateType,
    private val onDismiss: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val state: MessageBottomSheetUM = SendWithSwapRateInfoFactory.getRateTypeMessage(
        expressRateType = expressRateType,
        onDismiss = onDismiss,
    )

    override fun dismiss() = onDismiss()

    @Composable
    override fun BottomSheet() {
        MessageBottomSheet(state = state, onDismissRequest = ::dismiss)
    }
}