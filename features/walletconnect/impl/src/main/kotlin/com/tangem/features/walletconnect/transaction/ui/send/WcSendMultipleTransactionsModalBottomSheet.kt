package com.tangem.features.walletconnect.transaction.ui.send

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun WcSendMultipleTransactionsModalBottomSheet(
    config: TangemBottomSheetConfig,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent>(
        config = config,
        content = {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpacerH24()
                Icon(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(percent = 100))
                        .background(TangemTheme.colors.icon.informative.copy(alpha = 0.1f))
                        .padding(12.dp),
                    painter = rememberVectorPainter(
                        ImageVector.vectorResource(com.tangem.core.ui.R.drawable.ic_alert_24),
                    ),
                    tint = TangemTheme.colors.icon.attention,
                    contentDescription = null,
                )
                SpacerH24()
                Text(
                    text = "Multiple Transactions",
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )
                SpacerH8()
                Text(
                    text = "You’ll need to tap your Tangem device a few times to complete this process.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
                SpacerH(48.dp)
                SecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Return",
                    onClick = onBack,
                )
                SpacerH8()
                PrimaryButtonIconEnd(
                    modifier = Modifier.fillMaxWidth(),
                    iconResId = R.drawable.ic_tangem_24,
                    text = "Send",
                    onClick = onConfirm,
                )
            }
        },
    )
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSendTransactionBottomSheetPreview() {
    TangemThemePreview {
        WcSendMultipleTransactionsModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
            onConfirm = {},
            onBack = {},
        )
    }
}