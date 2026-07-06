package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.LocalTopSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_share_android_24
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.ui.state.ReceiveAssetsUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun TokenReceiveAssetsContent(assetsUM: ReceiveAssetsUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (assetsUM.isEnsResultLoading) {
            LoadingBlock(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            )
        }
        AddressBlock(assetsUM = assetsUM)
        SpacerH(16.dp)
        Info(
            showMemoDisclaimer = assetsUM.showMemoDisclaimer,
            notificationConfigs = assetsUM.notificationConfigs,
            currencyIconState = assetsUM.currencyIconState,
        )
    }
}

@Composable
private fun LoadingBlock(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors3.bg.opaque.primary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp),
            contentAlignment = Alignment.Center,
        ) {
            TangemLoader(
                color = TangemTheme.colors3.icon.secondary,
                size = TangemLoaderSize.X28,
            )
        }
    }
}

@Composable
private fun AddressBlock(assetsUM: ReceiveAssetsUM) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val topSnackbarHostState = LocalTopSnackbarHostState.current

    assetsUM.addresses
        .fastFilter { it.type is ReceiveAddress.Type.Ens }
        .fastForEach { address ->
            key(address.value) {
                EnsItem(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onCopyClick = {
                        assetsUM.onCopyClick(address)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            topSnackbarHostState.showSnackbar(
                                SnackbarMessage(
                                    startIconId = R.drawable.ic_check_24,
                                    message = resourceReference(R.string.wallet_notification_address_copied),
                                ),
                            )
                        }
                    },
                    address = address.value,
                )
                SpacerH8()
            }
        }

    PrimaryAddressesItems(
        addresses = assetsUM.addresses.fastFilter { it.type is ReceiveAddress.Type.Primary }.toImmutableList(),
        currencyIconState = assetsUM.currencyIconState,
        onShareClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            assetsUM.onShareClick(it)
        },
        onCopyClick = { address ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch {
                topSnackbarHostState.showSnackbar(
                    SnackbarMessage(message = resourceReference(R.string.wallet_notification_address_copied)),
                )
            }
            assetsUM.onCopyClick(address)
        },
        onOpenQrCodeClick = { address ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            assetsUM.onOpenQrCodeClick(address)
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun PrimaryAddressesItems(
    addresses: ImmutableList<ReceiveAddress>,
    currencyIconState: CurrencyIconState,
    onShareClick: (String) -> Unit,
    onCopyClick: (ReceiveAddress) -> Unit,
    onOpenQrCodeClick: (String) -> Unit,
) {
    if (addresses.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = addresses::count,
    )

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 16.dp,
    ) { page ->
        val address = addresses[page]
        AddressItem(
            currencyIconState = currencyIconState,
            onOpenQrCodeClick = { onOpenQrCodeClick(address.value) },
            onCopyClick = { onCopyClick(address) },
            onShareClick = { onShareClick(address.value) },
            primaryType = address.type as ReceiveAddress.Type.Primary,
            address = address.value,
            isDynamicAddress = address.type is ReceiveAddress.Type.Primary.Dynamic,
        )
    }
    SpacerH(8.dp)
    if (addresses.size > 1) {
        TangemPagerIndicator(pagerState = pagerState)
    }
}

@Suppress("LongParameterList")
@Composable
private fun AddressItem(
    currencyIconState: CurrencyIconState,
    onOpenQrCodeClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    primaryType: ReceiveAddress.Type.Primary,
    address: String,
    isDynamicAddress: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickableSingle(onClick = onOpenQrCodeClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CurrencyIcon(
            modifier = Modifier.size(60.dp),
            state = currencyIconState,
            shouldDisplayNetwork = true,
            iconSize = 56.dp,
            networkBadgeSize = 20.dp,
        )

        SpacerH(12.dp)

        if (isDynamicAddress) {
            DynamicAddressBadge(modifier = Modifier.padding(vertical = 4.dp))
            SpacerH(12.dp)
        }

        Text(
            text = primaryType.displayName.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.body.medium,
            textAlign = TextAlign.Center,
        )

        SpacerH(4.dp)

        Text(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp),
            text = address,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            textAlign = TextAlign.Center,
        )

        SpacerH(8.dp)

        Row(
            modifier = Modifier
                .clickableSingle(onClick = onOpenQrCodeClick)
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_qrcode_new_24),
                tint = TangemTheme.colors3.icon.primary,
                contentDescription = null,
            )

            SpacerW(6.dp)

            Text(
                text = stringResourceSafe(R.string.token_receive_show_qr_code_title),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }

        SpacerH(20.dp)

        ButtonsBlock(
            onCopyClick = onCopyClick,
            onShareClick = onShareClick,
        )
    }
}

@Composable
private fun ButtonsBlock(onCopyClick: () -> Unit, onShareClick: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val topSnackbarHostState = LocalTopSnackbarHostState.current

    Row(
        modifier = Modifier.width(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier.weight(1f),
            onClick = {
                onCopyClick()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                coroutineScope.launch {
                    topSnackbarHostState.showSnackbar(
                        SnackbarMessage(
                            startIconId = R.drawable.ic_check_24,
                            message = resourceReference(R.string.wallet_notification_address_copied),
                        ),
                    )
                }
            },
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_copy_24),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = TextReference.Res(id = R.string.common_copy),
        )

        TangemButton(
            modifier = Modifier.weight(1f),
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onShareClick()
            },
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_share_android_24),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = TextReference.Res(id = R.string.common_share),
        )
    }
}

@Composable
private fun EnsItem(onCopyClick: () -> Unit, address: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors3.bg.opaque.primary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(36.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_ens_36),
                contentDescription = null,
            )
            SpacerW(12.dp)

            Text(
                modifier = Modifier.weight(1f),
                text = address,
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.body.medium,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1,
            )

            TangemButton(
                modifier = Modifier.padding(start = 12.dp),
                onClick = onCopyClick,
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_share_android_24),
                size = TangemButton.Size.X9,
                variant = TangemButton.Variant.Secondary,
            )
        }
    }
}

@Composable
private fun DynamicAddressBadge(modifier: Modifier = Modifier) {
    TangemBadge(
        modifier = modifier,
        text = resourceReference(R.string.dynamic_addresses_receive_badge),
        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_dynamic_addresses_badge_16),
        status = TangemBadge.Status.Info,
    )
}

@Composable
private fun Info(
    currencyIconState: CurrencyIconState,
    notificationConfigs: ImmutableList<NotificationUM>,
    showMemoDisclaimer: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        if (showMemoDisclaimer) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.receive_bottom_sheet_no_memo_required_message),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }

        notificationConfigs.fastForEach { notificationConfig ->
            key(notificationConfig.hashCode()) {
                if (notificationConfig is NotificationUM.Warning.YieldSupplyIsActive) {
                    TangemMessage(
                        title = resourceReference(
                            id = R.string.yield_module_balance_info_sheet_title,
                            formatArgs = wrappedList(notificationConfig.tokenName),
                        ),
                        subtitle = resourceReference(R.string.yield_module_balance_info_sheet_subtitle),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .height(22.dp)
                                    .width(22.dp),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                CurrencyIcon(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .size(13.dp),
                                    state = currencyIconState,
                                    shouldDisplayNetwork = false,
                                    iconSize = 13.dp,
                                )
                                Image(
                                    modifier = Modifier
                                        .background(TangemTheme.colors.background.tertiary, RoundedCornerShape(15.dp))
                                        .padding(1.dp)
                                        .size(15.dp)
                                        .align(Alignment.BottomEnd),
                                    imageVector = ImageVector.vectorResource(R.drawable.img_aave_22),
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                } else {
                    TangemMessage(
                        config = notificationConfig.config,
                        contentColor = Color.Transparent,
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveAssetsContent(
    @PreviewParameter(TokenReceiveAssetsContentProvider::class) params: ReceiveAssetsUM,
) {
    TangemThemePreviewRedesign {
        TokenReceiveAssetsContent(assetsUM = params)
    }
}