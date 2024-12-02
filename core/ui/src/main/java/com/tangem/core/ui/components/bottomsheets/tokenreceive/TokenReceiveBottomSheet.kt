package com.tangem.core.ui.components.bottomsheets.tokenreceive

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.rememberQrPainters
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbarHost
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.shareText
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun TokenReceiveBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { content: TokenReceiveBottomSheetConfig ->
        TokenReceiveBottomSheetContent(content = content)
    }
}

@Composable
private fun TokenReceiveBottomSheetContent(content: TokenReceiveBottomSheetConfig) {
    var selectedAddress by remember { mutableStateOf(content.addresses.first()) }

    val snackbarHostState = remember(::SnackbarHostState)

    ContainerWithSnackbarHost(snackbarHostState = snackbarHostState) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(
                    start = TangemTheme.dimens.spacing24,
                    top = TangemTheme.dimens.spacing24,
                    end = TangemTheme.dimens.spacing24,
                    bottom = TangemTheme.dimens.spacing16,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing24),
        ) {
            QrCodeContent(content = content, onAddressChange = { selectedAddress = it })

            DisclaimerText(text = content.name)

            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16)) {
                CopyButton(
                    address = selectedAddress.value,
                    snackbarHostState = snackbarHostState,
                    onClick = content.onCopyClick,
                    modifier = Modifier.weight(1f),
                )

                ShareButton(
                    address = selectedAddress.value,
                    onClick = content.onShareClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ContainerWithSnackbarHost(snackbarHostState: SnackbarHostState, content: @Composable () -> Unit) {
    Box {
        content()

        CopiedTextSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing80),
        )
    }
}

@Composable
private fun QrCodeContent(content: TokenReceiveBottomSheetConfig, onAddressChange: (AddressModel) -> Unit) {
    val qrCodes = rememberQrPainters(content.addresses.map(AddressModel::value))

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = content.addresses::count,
    )

    LaunchedEffect(key1 = pagerState.currentPage) {
        onAddressChange.invoke(content.addresses[pagerState.currentPage])
    }

    HorizontalPager(state = pagerState) { currentPage ->
        QrCodePage(
            content = content,
            qrCodePainter = qrCodes[currentPage],
            currentIndex = currentPage,
        )
    }

    if (pagerState.pageCount > 1) {
        val indicatorState = rememberLazyListState()
        val selectedColor = TangemTheme.colors.icon.primary1
        val unselectedColor = TangemTheme.colors.icon.informative

        LazyRow(
            modifier = Modifier.height(TangemTheme.dimens.size20),
            state = indicatorState,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pagerState.pageCount) { iteration ->
                item(key = iteration) {
                    val color by animateColorAsState(
                        targetValue = if (pagerState.currentPage == iteration) selectedColor else unselectedColor,
                        label = "",
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = TangemTheme.dimens.spacing4, vertical = TangemTheme.dimens.spacing6)
                            .background(color = color, shape = CircleShape)
                            .size(TangemTheme.dimens.size7),
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCodePage(content: TokenReceiveBottomSheetConfig, qrCodePainter: Painter, currentIndex: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
    ) {
        Text(
            text = stringResource(
                R.string.receive_bottom_sheet_warning_message,
                getName(content = content, index = currentIndex),
                content.symbol,
                content.network,
            ),
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.h3,
        )

        Image(
            painter = qrCodePainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(TangemTheme.dimens.size248),
        )

        Text(
            text = content.addresses[currentIndex].value,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.subtitle1,
        )
    }
}

@Composable
private fun getName(content: TokenReceiveBottomSheetConfig, index: Int): String {
    return if (content.addresses.size < 2) {
        content.name
    } else {
        "${content.addresses[index].displayName.resolveReference()} ${content.name}"
    }
}

@Composable
private fun DisclaimerText(text: String) {
    Text(
        text = stringResource(R.string.receive_bottom_sheet_warning_message_full, text),
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
        style = TangemTheme.typography.caption2,
    )
}

@Composable
private fun CopyButton(
    address: String,
    snackbarHostState: SnackbarHostState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalContext.current.resources

    SecondaryButtonIconStart(
        modifier = modifier,
        text = stringResource(id = R.string.common_copy),
        iconResId = R.drawable.ic_copy_24,
        onClick = {
            onClick()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            clipboardManager.setText(AnnotatedString(address))

            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = resources.getString(R.string.wallet_notification_address_copied),
                )
            }
        },
    )
}

@Composable
private fun ShareButton(address: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    SecondaryButtonIconStart(
        modifier = modifier,
        text = stringResource(id = R.string.common_share),
        iconResId = R.drawable.ic_share_24,
        onClick = {
            onClick()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            context.shareText(address)
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveBottomSheet(
    @PreviewParameter(TokenReceiveBottomSheetConfigPreviewProvider::class) params: TangemBottomSheetConfig,
) {
    TangemThemePreview {
        TokenReceiveBottomSheet(params)
    }
}

private class TokenReceiveBottomSheetConfigPreviewProvider : PreviewParameterProvider<TangemBottomSheetConfig> {
    override val values: Sequence<TangemBottomSheetConfig>
        get() = sequenceOf(
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = {},
                content = TokenReceiveBottomSheetConfig(
                    name = "Stellar",
                    symbol = "XLM",
                    network = "Ethereum",
                    addresses = persistentListOf(
                        AddressModel(
                            displayName = stringReference("Address 1"),
                            value = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
                            type = AddressModel.Type.Default,
                        ),
                    ),
                    onCopyClick = {},
                    onShareClick = {},
                ),
            ),
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = {},
                content = TokenReceiveBottomSheetConfig(
                    name = "Stellar",
                    symbol = "XLM",
                    network = "Ethereum",
                    addresses = persistentListOf(
                        AddressModel(
                            displayName = stringReference("Address 1"),
                            value = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
                            type = AddressModel.Type.Default,
                        ),
                        AddressModel(
                            displayName = stringReference("Address 2"),
                            value = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
                            type = AddressModel.Type.Legacy,
                        ),
                    ),
                    onCopyClick = {},
                    onShareClick = {},
                ),
            ),
        )
}
// endregion Preview