package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.utils.toPx
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.extensions.stripZeroPlainString
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal

@Composable
internal fun BoxScope.ExpressShareContent(state: ExpressTransactionStateUM) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var graphicsLayer = rememberGraphicsLayer()

    val shareText = makeExpressShareContent(state) ?: return

    TangemThemeRedesign {
        SecondaryButtonIconStart(
            text = stringResourceSafe(R.string.common_share),
            iconResId = R.drawable.ic_share_24,
            onClick = {
                coroutineScope.launch {
                    if (graphicsLayer.size.width > 0 && graphicsLayer.size.height > 0) {
                        val uri = graphicsLayer.saveAsShareableFile(context)

                        val shareIntent = if (uri != null) {
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Temporary external read grant
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newRawUri(null, uri)

                                type = "image/png"
                            }
                        } else {
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
        )
        ExpressShareImageContent(
            state = state,
            onGraphicsLayer = { layer -> graphicsLayer = layer },
            modifier = Modifier.size(0.dp), // size 0 so that no space is used in the UI
        )
    }
}

@Composable
private fun ExpressShareImageContent(
    state: ExpressTransactionStateUM,
    onGraphicsLayer: (GraphicsLayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.drawForShare(onGraphicsLayer)) {
        Box(
            // override the parent size with desired size of the recording
            modifier = Modifier
                .wrapContentHeight(unbounded = true, align = Alignment.Top)
                .wrapContentWidth(unbounded = true, align = Alignment.Start)
                .requiredSize(525.dp, 580.dp),
        ) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .background(TangemColorPalette.Black),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 40.dp,
                        end = 40.dp,
                        top = 40.dp,
                        bottom = 20.dp,
                    ),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.img_tangem_logo_90_24),
                        contentDescription = null,
                        tint = TangemColorPalette.White,
                    )
                    SpacerH(36.dp)

                    ExpressShareImageAmount(
                        prefix = stringResourceSafe(R.string.common_send),
                        amountValue = state.info.fromAmountValue,
                        currencyIconState = state.info.fromCurrencyIcon,
                        currencySymbol = state.info.fromAmountSymbol,
                    )
                    ExpressShareImageAddress(
                        prefix = stringResourceSafe(R.string.common_from),
                        address = state.info.fromAddress,
                    )

                    SpacerH(24.dp)
                    ExpressShareImageSeparator()
                    SpacerH(24.dp)

                    ExpressShareImageAmount(
                        prefix = stringResourceSafe(R.string.common_receive),
                        amountValue = state.info.toAmountValue,
                        currencyIconState = state.info.toCurrencyIcon,
                        currencySymbol = state.info.toAmountSymbol,
                    )
                    ExpressShareImageAddress(
                        prefix = stringResourceSafe(R.string.common_to),
                        address = state.info.toAddress,
                    )
                    SpacerH(24.dp)

                    ExpressShareImageProvider(state)
                    Text(
                        text = stringResourceSafe(R.string.express_transaction_id, state.info.txExternalId.orEmpty()),
                        style = TangemTheme.typography2.bodySemibold16,
                        color = TangemTheme.colors3.text.staticDark.secondary,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.img_share_express_background),
                    contentDescription = null,
                    modifier = Modifier,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ExpressShareImageAmount(
    prefix: String,
    amountValue: BigDecimal,
    currencyIconState: CurrencyIconState,
    currencySymbol: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = prefix,
            color = TangemTheme.colors3.text.staticDark.primary,
            style = TangemTheme.typography2.bodySemibold16,
        )
        Text(
            text = amountValue.stripZeroPlainString(),
            color = TangemTheme.colors3.text.staticDark.primary,
            style = TangemTheme.typography2.bodySemibold16,
        )
        CurrencyIcon(
            state = currencyIconState,
            iconSize = 20.dp,
            shouldDisplayNetwork = false,
        )
        Text(
            text = currencySymbol,
            color = TangemTheme.colors3.text.staticDark.primary,
            style = TangemTheme.typography2.bodySemibold16,
        )
    }
}

@Composable
private fun ExpressShareImageAddress(prefix: String, address: String?) {
    if (address != null) {
        Text(
            text = "$prefix:",
            color = TangemTheme.colors3.text.staticDark.secondary,
        )
        Text(
            text = address,
            color = TangemTheme.colors3.text.staticDark.secondary,
        )
    }
}

@Composable
private fun ExpressShareImageSeparator() {
    val width = 1.dp
    val height = 39.dp
    val dashOnInterval = (width * 2).toPx()
    val dashOffInterval = (width * 2).toPx()

    val pathEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(dashOnInterval, dashOffInterval),
    )
    Canvas(modifier = Modifier.size(width = width, height = height)) {
        drawLine(
            color = TangemColorPalette.Dark2,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = width.toPx(),
            cap = StrokeCap.Round,
            pathEffect = pathEffect,
        )
    }
}

@Composable
private fun ExpressShareImageProvider(state: ExpressTransactionStateUM) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.express_by_provider),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors3.text.staticDark.primary,
        )
        val providerImageUrl = when (state) {
            is ExchangeUM -> state.provider.imageLarge
            is ExpressTransactionStateUM.OnrampUM -> state.providerImageUrl
            else -> null
        }
        val providerName = when (state) {
            is ExchangeUM -> state.provider.name
            is ExpressTransactionStateUM.OnrampUM -> state.providerName
            else -> null
        }
        val providerType = when (state) {
            is ExchangeUM -> state.provider.type.providerName
            is ExpressTransactionStateUM.OnrampUM -> state.providerType
            else -> null
        }
        if (providerImageUrl != null) {
            TangemIcon(
                tangemIconUM = TangemIconUM.Url(providerImageUrl, fallbackRes = R.drawable.ic_empty_64),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = providerName.orEmpty(),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors3.text.staticDark.primary,
        )
        Text(
            text = providerType.orEmpty(),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors3.text.staticDark.secondary,
        )
    }
}

@Composable
private fun Modifier.drawForShare(onGraphicsLayer: (GraphicsLayer) -> Unit): Modifier {
    val isInspectionMode = LocalInspectionMode.current

    return drawWithCache {
        // draw to graphics layer
        onGraphicsLayer(
            obtainGraphicsLayer().apply {
                record(
                    size = IntSize(
                        width = 525.dp.toPx().toInt(),
                        height = 580.dp.toPx().toInt(),
                    ),
                ) {
                    drawContent()
                }
            },
        )

        if (isInspectionMode) {
            // draw only for preview mode
            onDrawWithContent { drawContent() }
        } else {
            // leave blank to skip drawing on the screen
            onDrawWithContent { }
        }
    }
}

@Suppress("MagicNumber")
private suspend fun GraphicsLayer.saveAsShareableFile(context: Context): Uri? {
    // convert to bitmap
    val bitmap = this.toImageBitmap().asAndroidBitmap()

    // create file
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdir()

    val file = File(cachePath, "shared_image.png")

    // write bitmap to file as PNG
    file.outputStream().use { out ->
        bitmap.compress(/* format = */ Bitmap.CompressFormat.PNG, /* quality = */ 100, /* stream = */ out)
        out.flush()
    }

    // Generate secure Content URI using the registered authority
    return FileProvider.getUriForFile(
        /* context = */ context,
        /* authority = */ "${context.packageName}.provider",
        /* file = */ file,
    )
}

@ReadOnlyComposable
@Composable
internal fun makeExpressShareContent(state: ExpressTransactionStateUM): String? {
    val txExternalId = state.info.txExternalId
    val txId = if (txExternalId != null) {
        stringResourceSafe(R.string.express_transaction_id, txExternalId)
    } else {
        ""
    }

    val (providerName, providerType) = when (state) {
        is ExchangeUM -> state.provider.name to state.provider.type
        is ExpressTransactionStateUM.OnrampUM -> state.providerName to state.providerType
        else -> return null
    }
    val providerInfo = "${stringResourceSafe(R.string.express_by_provider)} $providerName $providerType"
    val fromAddress = if (state.info.fromAddress != null) {
        "${stringResourceSafe(R.string.common_from)}: ${state.info.fromAddress}"
    } else {
        ""
    }
    val text = """
                ${stringResourceSafe(R.string.common_tangem)}
                
                ${stringResourceSafe(R.string.common_send)} ${state.info.fromAmount.resolveReference()}
                $fromAddress
                
                ${stringResourceSafe(R.string.common_receive)} ${state.info.toAmount.resolveReference()}
                ${stringResourceSafe(R.string.common_to)}: ${state.info.toAddress}
                
                $providerInfo
                $txId
    """.trimIndent()

    return text
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, widthDp = 360, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ExpressShareImageContent_Preview(
    @PreviewParameter(ExpressStatusBottomSheetStateProvider::class) param: ExpressStatusBottomSheetConfig,
) {
    TangemThemePreviewRedesign {
        ExpressShareImageContent(param.value, {}, modifier = Modifier)
    }
}

// endregion