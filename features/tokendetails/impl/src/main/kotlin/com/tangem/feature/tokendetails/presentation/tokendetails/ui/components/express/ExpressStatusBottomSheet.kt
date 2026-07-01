package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.OnrampStatusBottomSheetContent
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.exchange.ExchangeStatusBottomSheetContent

@Composable
internal fun ExpressStatusBottomSheet(
    config: TangemBottomSheetConfig,
    isExpressShareButtonEnabled: Boolean,
    extraContent: (@Composable () -> Unit)? = null,
) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: ExpressStatusBottomSheetConfig ->
        if (isExpressShareButtonEnabled) {
            Box {
                when (val state = content.value) {
                    is ExpressTransactionStateUM.OnrampUM -> OnrampStatusBottomSheetContent(
                        state = state,
                        isExpressShareButtonEnabled = true,
                    )
                    is ExchangeUM -> ExchangeStatusBottomSheetContent(
                        state = state,
                        extraContent = extraContent,
                        isExpressShareButtonEnabled = true,
                    )
                }
                BottomFade(
                    backgroundColor = TangemTheme.colors.background.tertiary,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                ExpressShareContent(state = content.value)
            }
        } else {
            when (val state = content.value) {
                is ExpressTransactionStateUM.OnrampUM -> OnrampStatusBottomSheetContent(
                    state = state,
                    isExpressShareButtonEnabled = false,
                )
                is ExchangeUM -> ExchangeStatusBottomSheetContent(
                    state = state,
                    extraContent = extraContent,
                    isExpressShareButtonEnabled = false,
                )
            }
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
            isExpressShareButtonEnabled = false,
        )
    }
}