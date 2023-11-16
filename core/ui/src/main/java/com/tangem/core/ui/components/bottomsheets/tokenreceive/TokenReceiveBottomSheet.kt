package com.tangem.core.ui.components.bottomsheets.tokenreceive

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.rememberQrPainters
import com.tangem.core.ui.extensions.shareText
import com.tangem.core.ui.res.TangemTheme

@Composable
fun TokenReceiveBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { content: TokenReceiveBottomSheetConfig ->
        TokenReceiveBottomSheetContent(content = content)
    }
}

@Composable
private fun TokenReceiveBottomSheetContent(content: TokenReceiveBottomSheetConfig) {
    var selectedAddress by remember { mutableStateOf(content.addresses.first()) }
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
        QrCodeContent(
            content = content,
            onAddressChange = { selectedAddress = it },
        )
        Text(
            text = stringResource(R.string.receive_bottom_sheet_warning_message_full, content.name),
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.caption2,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            val clipboardManager = LocalClipboardManager.current
            val hapticFeedback = LocalHapticFeedback.current
            val context = LocalContext.current
            SecondaryButtonIconStart(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.common_copy),
                iconResId = R.drawable.ic_copy_24,
                onClick = {
                    content.onCopyClick.invoke()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboardManager.setText(AnnotatedString(selectedAddress.value))
                    Toast.makeText(context, R.string.wallet_notification_address_copied, Toast.LENGTH_SHORT).show()
                },
            )
            SecondaryButtonIconStart(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.common_share),
                iconResId = R.drawable.ic_share_24,
                onClick = {
                    content.onShareClick.invoke()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    context.shareText(selectedAddress.value)
                },
            )
        }
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QrCodeContent(content: TokenReceiveBottomSheetConfig, onAddressChange: (AddressModel) -> Unit) {
    val qrCodes = rememberQrPainters(content.addresses.map(AddressModel::value))
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
    ) {
        content.addresses.count()
    }

    LaunchedEffect(key1 = pagerState.currentPage) {
        onAddressChange.invoke(content.addresses[pagerState.currentPage])
    }

    HorizontalPager(
        state = pagerState,
    ) { currentPage ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
        ) {
            Text(
                text = stringResource(
                    R.string.receive_bottom_sheet_warning_message,
                    getName(content = content, index = pagerState.currentPage),
                    content.symbol,
                    content.network,
                ),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h3,
            )
            Image(
                painter = qrCodes[currentPage],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(TangemTheme.dimens.size248),
            )
            Text(
                text = content.addresses[currentPage].value,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.subtitle1,
            )
        }
    }

    if (pagerState.pageCount > 1) {
        val indicatorState = rememberLazyListState()
        val selectedColor = TangemTheme.colors.icon.primary1
        val unselectedColor = TangemTheme.colors.icon.informative
        LazyRow(
            modifier = Modifier
                .height(TangemTheme.dimens.size20),
            state = indicatorState,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pagerState.pageCount) { iteration ->
                item(key = iteration) {
                    val color by animateColorAsState(
                        if (pagerState.currentPage == iteration) selectedColor else unselectedColor,
                    )
                    Box(
                        modifier = Modifier
                            .padding(
                                start = TangemTheme.dimens.spacing4,
                                top = TangemTheme.dimens.spacing6,
                                end = TangemTheme.dimens.spacing4,
                                bottom = TangemTheme.dimens.spacing6,
                            )
                            .background(color, CircleShape)
                            .size(TangemTheme.dimens.size7),
                    )
                }
            }
        }
    }
}

@Composable
private fun getName(content: TokenReceiveBottomSheetConfig, index: Int): String {
    return if (content.addresses.size < 2) {
        content.name
    } else {
        "${
        stringResource(
            id = when (content.addresses[index].type) {
                AddressModel.Type.Default -> R.string.address_type_default
                AddressModel.Type.Legacy -> R.string.address_type_legacy
            },
        )
        } ${content.name}"
    }
}