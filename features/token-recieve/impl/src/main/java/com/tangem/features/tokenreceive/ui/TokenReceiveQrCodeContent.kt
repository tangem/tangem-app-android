package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.res.getStringSafe
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.QrCodeUM
import kotlinx.coroutines.launch

@Composable
internal fun TokenReceiveQrCodeContent(qrCodeUM: QrCodeUM) {
    val snackbarHostState = remember(::SnackbarHostState)
    val qrCodePainter = rememberQrPainter(content = qrCodeUM.addressValue)

    ContainerWithSnackbarHost(snackbarHostState = snackbarHostState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.tertiary)
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpacerH8()

            QrCodePage(
                addressFullName = qrCodeUM.addressName,
                addressValue = qrCodeUM.addressValue,
                network = qrCodeUM.network,
                qrCodePainter = qrCodePainter,
            )
            SpacerH24()

            Buttons(
                onShareClick = { qrCodeUM.onShareClick(qrCodeUM.addressValue) },
                onCopyClick = qrCodeUM.onCopyClick,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

@Composable
private fun QrCodePage(addressFullName: TextReference, addressValue: String, network: String, qrCodePainter: Painter) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.size36),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(
                    R.string.receive_bottom_sheet_warning_message_compact,
                    addressFullName.resolveReference(),
                    network,
                ),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h3,
            )

            SpacerH(20.dp)

            Box(
                modifier = Modifier
                    .border(
                        width = 8.dp,
                        color = TangemTheme.colors.icon.constant,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(8.dp),

            ) {
                Image(
                    painter = qrCodePainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.sizeIn(minWidth = 148.dp),
                )
            }

            SpacerH24()
        }
        Text(
            text = stringResourceSafe(R.string.wc_common_address),
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.subtitle2,
        )

        SpacerH2()

        Text(
            text = addressValue,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.subtitle1,
        )
    }
}

@Composable
private fun Buttons(
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = context.resources

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SecondaryButtonIconStart(
            modifier = Modifier.weight(1f),
            text = stringResourceSafe(id = R.string.common_copy),
            iconResId = R.drawable.ic_copy_24,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onCopyClick()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = resources.getStringSafe(R.string.wallet_notification_address_copied),
                    )
                }
            },
        )

        SecondaryButtonIconStart(
            modifier = Modifier.weight(1f),
            text = stringResourceSafe(id = R.string.common_share),
            iconResId = R.drawable.ic_share_24,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onShareClick()
            },
        )
    }
}

@Composable
private fun rememberQrPainter(content: String, size: Dp = 248.dp, padding: Dp = 0.dp): BitmapPainter {
    val density = LocalDensity.current
    return remember(content) {
        BitmapPainter(
            content.toQrCode(
                sizePx = with(density) { size.roundToPx() },
                paddingPx = with(density) { padding.roundToPx() },
            ).asImageBitmap(),
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveQrCodeContent(
    @PreviewParameter(TokenReceiveQrCodeContentPreviewProvider::class) qrCodeUM: QrCodeUM,
) {
    TangemThemePreview {
        TokenReceiveQrCodeContent(qrCodeUM = qrCodeUM)
    }
}

private class TokenReceiveQrCodeContentPreviewProvider : PreviewParameterProvider<QrCodeUM> {
    private val config = QrCodeUM(
        network = "Ethereum",
        addressName = stringReference("Etherium"),
        addressValue = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
        onCopyClick = {},
        onShareClick = {},
    )

    override val values: Sequence<QrCodeUM>
        get() = sequenceOf(config)
}