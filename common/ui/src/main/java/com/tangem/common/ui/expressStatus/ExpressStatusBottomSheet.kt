package com.tangem.common.ui.expressStatus

import androidx.compose.runtime.Composable
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.res.TangemTheme

data class ExpressStatusBottomSheetConfig(
    val value: ExpressTransactionStateUM,
) : TangemBottomSheetConfigContent

@Composable
fun ExpressStatusBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: ExpressStatusBottomSheetConfig ->
        when (val state = content.value) {
            is ExpressTransactionStateUM.OnrampUM -> OnrampStatusBottomSheetContent(state)
        }
    }
}