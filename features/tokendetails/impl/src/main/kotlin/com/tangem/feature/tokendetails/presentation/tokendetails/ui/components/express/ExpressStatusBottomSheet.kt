package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.OnrampStatusBottomSheetContent
import com.tangem.common.ui.expressStatus.state.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
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

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewExpressStatusBottomSheet(
    @PreviewParameter(ExpressStatusBottomSheetStateProvider::class) param: ExpressStatusBottomSheetConfig,
) {
    TangemThemePreview {
        ExpressStatusBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = param,
            ),
        )
    }
}