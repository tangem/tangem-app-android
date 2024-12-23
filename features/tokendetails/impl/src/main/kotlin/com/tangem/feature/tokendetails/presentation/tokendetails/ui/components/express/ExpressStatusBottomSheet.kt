package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express

import androidx.compose.runtime.Composable
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.OnrampStatusBottomSheetContent
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.exchange.ExchangeStatusBottomSheetContent

@Composable
internal fun ExpressStatusBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: ExpressStatusBottomSheetConfig ->
        when (val state = content.value) {
            is ExpressTransactionStateUM.OnrampUM -> OnrampStatusBottomSheetContent(state)
            is ExchangeUM -> ExchangeStatusBottomSheetContent(state)
        }
    }
}