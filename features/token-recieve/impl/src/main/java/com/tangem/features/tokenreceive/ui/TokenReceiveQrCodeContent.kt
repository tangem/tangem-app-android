package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.LocalTopSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_share_android_24
import com.tangem.core.ui.test.TokenReceiveQrCodeBottomSheetTestTags
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.QrCodeUM
import kotlinx.coroutines.launch

@Composable
internal fun TokenReceiveQrCodeContent(qrCodeUM: QrCodeUM) {
    val qrCodePainter = rememberQrPainter(content = qrCodeUM.addressValue)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QrCodePage(
            addressFullName = qrCodeUM.addressName,
            addressValue = qrCodeUM.addressValue,
            network = qrCodeUM.network,
            qrCodePainter = qrCodePainter,
        )

        SpacerH(24.dp)

        Buttons(
            modifier = Modifier.padding(top = 8.dp),
            onShareClick = { qrCodeUM.onShareClick(qrCodeUM.addressValue) },
            onCopyClick = qrCodeUM.onCopyClick,
        )
    }
}

@Composable
private fun QrCodePage(addressFullName: TextReference, addressValue: String, network: String, qrCodePainter: Painter) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 16.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(
                    R.string.receive_bottom_sheet_warning_message_compact,
                    addressFullName.resolveReference(),
                    network,
                ),
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography3.heading.small,
                modifier = Modifier.testTag(TokenReceiveQrCodeBottomSheetTestTags.TITLE),
            )

            SpacerH(32.dp)

            Box(
                modifier = Modifier
                    .border(
                        width = 16.dp,
                        color = TangemTheme.colors3.bg.secondary,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(16.dp)
                    .testTag(TokenReceiveQrCodeBottomSheetTestTags.QR_CODE),

            ) {
                Image(
                    painter = qrCodePainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.sizeIn(minWidth = 148.dp),
                )
            }
        }
        Text(
            text = stringResourceSafe(R.string.wc_common_address),
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography3.caption.medium,
        )

        SpacerH(4.dp)

        Text(
            text = addressValue,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography3.body.medium,
            modifier = Modifier.testTag(TokenReceiveQrCodeBottomSheetTestTags.ADDRESS),
        )
    }
}

@Composable
private fun Buttons(onShareClick: () -> Unit, onCopyClick: () -> Unit, modifier: Modifier = Modifier) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val tangemTopSnackbarHostState = LocalTopSnackbarHostState.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier.weight(1f),
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onCopyClick()
                coroutineScope.launch {
                    tangemTopSnackbarHostState.showSnackbar(
                        SnackbarMessage(
                            startIconId = R.drawable.ic_check_24,
                            message = resourceReference(R.string.wallet_notification_address_copied),
                        ),
                    )
                }
            },
            text = resourceReference(R.string.common_copy),
            iconStart = TangemIconUM.Icon(Icons.ic_copy_24),
            size = TangemButton.Size.X12,
            variant = TangemButton.Variant.Secondary,
        )

        TangemButton(
            modifier = Modifier.weight(1f),
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onShareClick()
            },
            text = resourceReference(R.string.common_share),
            iconStart = TangemIconUM.Icon(Icons.ic_share_android_24),
            size = TangemButton.Size.X12,
            variant = TangemButton.Variant.Secondary,
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
    TangemThemePreviewRedesign {
        TokenReceiveQrCodeContent(qrCodeUM = qrCodeUM)
    }
}