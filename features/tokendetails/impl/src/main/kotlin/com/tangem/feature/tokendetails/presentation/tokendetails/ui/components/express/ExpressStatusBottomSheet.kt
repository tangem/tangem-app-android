package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.exchange.ExchangeStatusBottomSheetContent
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.onramp.OnrampStatusBottomSheetContent

internal data class ExpressStatusBottomSheetConfig(
    val value: ExpressTransactionStateUM,
) : TangemBottomSheetConfigContent

@Composable
internal fun ExpressStatusBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: ExpressStatusBottomSheetConfig ->
        when (val state = content.value) {
            is ExpressTransactionStateUM.OnrampUM -> OnrampStatusBottomSheetContent(state)
            is ExpressTransactionStateUM.ExchangeUM -> ExchangeStatusBottomSheetContent(state)
        }
    }
}