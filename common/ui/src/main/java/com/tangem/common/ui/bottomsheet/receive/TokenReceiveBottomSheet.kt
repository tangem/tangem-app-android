package com.tangem.common.ui.bottomsheet.receive

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.res.getStringSafe
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.rememberQrPainters
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbarHost
import com.tangem.core.ui.extensions.*
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
                .padding(top = 24.dp, bottom = 16.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 24.dp),
        ) {
            QrCodeContent(content = content, onAddressChange = { selectedAddress = it })

            Info(
                currency = content.symbol,
                network = content.network,
                showMemoDisclaimer = content.showMemoDisclaimer,
            )

            Buttons(
                snackbarHostState = snackbarHostState,
                onShareClick = { content.onShareClick(selectedAddress.value) },
                onCopyClick = { content.onCopyClick(selectedAddress.value) },
            )
        }
    }
}

@Composable
private fun Info(currency: String, network: String, showMemoDisclaimer: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        if (showMemoDisclaimer) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.receive_bottom_sheet_no_memo_required_message),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }

        Notification(
            config = NotificationConfig(
                title = resourceReference(
                    R.string.receive_bottom_sheet_warning_title,
                    wrappedList(currency, network),
                ),
                subtitle = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
                iconResId = R.drawable.ic_alert_circle_24,
            ),
            iconTint = TangemTheme.colors.icon.accent,
        )
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
                .padding(bottom = 80.dp),
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
            modifier = Modifier.height(20.dp),
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
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .background(color = color, shape = CircleShape)
                            .size(7.dp),
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
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResourceSafe(
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
            modifier = Modifier.size(248.dp),
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
                onCopyClick()

                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

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

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveBottomSheet(
    @PreviewParameter(TokenReceiveBottomSheetConfigPreviewProvider::class) params: TokenReceiveBottomSheetConfig,
) {
    TangemThemePreview {
        val config = TangemBottomSheetConfig(
            isShown = true,
            content = params,
            onDismissRequest = {},
        )

        TokenReceiveBottomSheet(config)
    }
}

private class TokenReceiveBottomSheetConfigPreviewProvider : PreviewParameterProvider<TokenReceiveBottomSheetConfig> {
    val address = AddressModel(
        displayName = stringReference("Address 1"),
        value = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
        type = AddressModel.Type.Default,
    )
    private val baseConfig = TokenReceiveBottomSheetConfig(
        name = "Stellar",
        symbol = "XLM",
        network = "Ethereum",
        addresses = persistentListOf(address),
        showMemoDisclaimer = false,
        onCopyClick = {},
        onShareClick = {},
    )

    override val values: Sequence<TokenReceiveBottomSheetConfig>
        get() = sequenceOf(
            baseConfig,
            baseConfig.copy(
                addresses = persistentListOf(
                    address,
                    address.copy(displayName = stringReference("Address 2")),
                ),
            ),
            baseConfig.copy(
                showMemoDisclaimer = true,
            ),
            baseConfig.copy(
                showMemoDisclaimer = true,
                addresses = persistentListOf(
                    address,
                    address.copy(displayName = stringReference("Address 2")),
                ),
            ),
        )
}
// endregion Preview