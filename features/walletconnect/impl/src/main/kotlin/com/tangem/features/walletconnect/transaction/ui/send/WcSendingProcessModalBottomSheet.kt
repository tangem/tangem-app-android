package com.tangem.features.walletconnect.transaction.ui.send

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun WcSendingProcessModalBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent>(
        config = config,
        onBack = {
            // empty to disable back handling
        },
        dismissOnClickOutside = false,
        content = {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpacerH(64.dp)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size32),
                    color = TangemTheme.colors.text.accent,
                    strokeWidth = TangemTheme.dimens.size2,
                )
                SpacerH(32.dp)
                Text(
                    text = "Sending your funds",
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )
                SpacerH8()
                Text(
                    text = "We’re processing the transaction — it’ll be complete in moments.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
                SpacerH(56.dp)
            }
        },
    )
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSendTransactionBottomSheetPreview() {
    TangemThemePreview {
        WcSendingProcessModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
        )
    }
}