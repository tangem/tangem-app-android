package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.res.getStringSafe
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.buttons.actions.ActionBaseButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.ActionButtonContent
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.ui.state.ReceiveAssetsUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun TokenReceiveAssetsContent(assetsUM: ReceiveAssetsUM) {
    val snackbarHostState = remember(::SnackbarHostState)

    ContainerWithSnackbarHost(snackbarHostState = snackbarHostState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.tertiary)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddressBlock(
                assetsUM = assetsUM,
                snackbarHostState = snackbarHostState,
            )

            if (assetsUM.isEnsResultLoading) {
                SpacerH8()
                LoadingBlock(modifier = Modifier.padding(horizontal = 16.dp))
            }

            SpacerH12()

            Info(
                showMemoDisclaimer = assetsUM.showMemoDisclaimer,
                notificationConfigs = assetsUM.notificationConfigs,
                currencyIconState = assetsUM.currencyIconState,
            )
        }
    }
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
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }

        notificationConfigs.fastForEach { notificationConfig ->
            key(notificationConfig.hashCode()) {
                // TODO remove after new design system
                if (notificationConfig is NotificationUM.Warning.YieldSupplyIsActive) {
                    YieldSupplyDepositedWarning(
                        currencyIconState = currencyIconState,
                        title = stringResourceSafe(
                            R.string.yield_module_balance_info_sheet_title,
                            notificationConfig.tokenName,
                        ),
                        subtitle = stringResourceSafe(R.string.yield_module_balance_info_sheet_subtitle),
                    )
                } else {
                    Notification(config = notificationConfig.config)
                }
            }
        }
    }
}

@Composable
private fun YieldSupplyDepositedWarning(
    currencyIconState: CurrencyIconState,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size44)
            .fillMaxWidth(),
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.button.disabled,
    ) {
        Row(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing12),
        ) {
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

            SpacerW(width = TangemTheme.dimens.spacing8)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.button,
                )

                SpacerH(height = TangemTheme.dimens.spacing2)

                Text(
                    text = subtitle,
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.caption2,
                )
            }
        }
    }
}

@Composable
private fun AddressBlock(assetsUM: ReceiveAssetsUM, snackbarHostState: SnackbarHostState) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = context.resources

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
                            snackbarHostState.showSnackbar(
                                message = resources.getStringSafe(
                                    R.string.wallet_notification_address_copied,
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
        snackbarHostState = snackbarHostState,
        onShareClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            assetsUM.onShareClick(it)
        },
        onCopyClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = resources.getStringSafe(
                        R.string.wallet_notification_address_copied,
                    ),
                )
            }
            assetsUM.onCopyClick(it)
        },
        onOpenQrCodeClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            assetsUM.onOpenQrCodeClick(it)
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun PrimaryAddressesItems(
    addresses: ImmutableList<ReceiveAddress>,
    currencyIconState: CurrencyIconState,
    snackbarHostState: SnackbarHostState,
    onShareClick: (String) -> Unit,
    onCopyClick: (ReceiveAddress) -> Unit,
    onOpenQrCodeClick: (String) -> Unit,
) {
    if (addresses.isEmpty()) return
    var selectedAddress by remember { mutableStateOf(addresses.first()) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = addresses::count,
    )
    LaunchedEffect(key1 = pagerState.currentPage) {
        selectedAddress = addresses[pagerState.currentPage]
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 16.dp,
    ) {
        AddressItem(
            currencyIconState = currencyIconState,
            onOpenQrCodeClick = { onOpenQrCodeClick(selectedAddress.value) },
            onCopyClick = { onCopyClick(selectedAddress) },
            onShareClick = { onShareClick(selectedAddress.value) },
            primaryType = selectedAddress.type as ReceiveAddress.Type.Primary,
            address = selectedAddress.value,
            snackbarHostState = snackbarHostState,
        )
    }

    SpacerH(4.dp)

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

@Suppress("LongParameterList")
@Composable
private fun AddressItem(
    currencyIconState: CurrencyIconState,
    onOpenQrCodeClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    primaryType: ReceiveAddress.Type.Primary,
    address: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors.background.action),
        onClick = onOpenQrCodeClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CurrencyIcon(
                modifier = Modifier.size(56.dp),
                state = currencyIconState,
                shouldDisplayNetwork = true,
                iconSize = 56.dp,
            )

            SpacerH(12.dp)

            Text(
                text = primaryType.displayName.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
                textAlign = TextAlign.Center,
            )

            Text(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 16.dp),
                text = address,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            SpacerH8()

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenQrCodeClick)
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_qrcode_new_24),
                    contentDescription = null,
                )

                SpacerW(4.dp)

                Text(
                    text = stringResourceSafe(R.string.token_receive_show_qr_code_title),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.secondary,
                )
            }

            SpacerH(20.dp)

            ButtonsBlock(
                snackbarHostState = snackbarHostState,
                onCopyClick = onCopyClick,
                onShareClick = onShareClick,
            )
        }
    }
}

@Composable
private fun ButtonsBlock(snackbarHostState: SnackbarHostState, onCopyClick: () -> Unit, onShareClick: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = context.resources

    Row(
        modifier = Modifier.width(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButtonWithResizableText(
            modifier = Modifier.weight(1f),
            config = ActionButtonConfig(
                text = TextReference.Res(id = R.string.common_copy),
                iconResId = R.drawable.ic_copy_new_24,
                onClick = {
                    onCopyClick()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = resources.getStringSafe(R.string.wallet_notification_address_copied),
                        )
                    }
                },
            ),
        )

        ActionButtonWithResizableText(
            modifier = Modifier.weight(1f),
            config = ActionButtonConfig(
                text = TextReference.Res(id = R.string.common_share),
                iconResId = R.drawable.ic_share_24,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShareClick()
                },
            ),
        )
    }
}

@Composable
private fun EnsItem(onCopyClick: () -> Unit, address: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors.background.action),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(36.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_ens_36),
                contentDescription = null,
            )

            SpacerW12()

            EllipsisText(
                modifier = Modifier.weight(1f),
                text = address,
                ellipsis = TextEllipsis.Middle,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
            )

            TangemIconButton(
                modifier = Modifier.size(TangemTheme.dimens.size28),
                iconRes = R.drawable.ic_share_24,
                innerPadding = 6.dp,
                onClick = onCopyClick,
            )
        }
    }
}

@Composable
private fun LoadingBlock(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors.background.action),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = TangemTheme.colors.icon.informative,
                modifier = Modifier.padding(TangemTheme.dimens.spacing8),
            )
        }
    }
}

@Composable
private fun ActionButtonWithResizableText(config: ActionButtonConfig, modifier: Modifier = Modifier) {
    ActionBaseButton(
        config = config,
        shape = RoundedCornerShape(size = TangemTheme.dimens.radius24),
        content = { contentModifier ->
            ActionButtonContent(
                config = config,
                text = { color ->
                    Text(
                        text = config.text.resolveReference(),
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 10.sp,
                            maxFontSize = TangemTheme.typography.button.fontSize,
                        ),
                        color = color,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = TangemTheme.typography.button,
                    )
                },
                modifier = contentModifier.padding(horizontal = 16.dp),
                paddingBetweenIconAndText = 4.dp,
            )
        },
        modifier = modifier,
        color = TangemTheme.colors.button.secondary,
    )
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveAssetsContent(
    @PreviewParameter(TokenReceiveAssetsContentProvider::class) params: ReceiveAssetsUM,
) {
    TangemThemePreview {
        TokenReceiveAssetsContent(assetsUM = params)
    }
}

private class TokenReceiveAssetsContentProvider : PreviewParameterProvider<ReceiveAssetsUM> {
    val address = ReceiveAddress(
        value = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
        type = ReceiveAddress.Type.Primary.Default(
            displayName = stringReference("Etherium address"),
        ),
    )
    private val config = ReceiveAssetsUM(
        notificationConfigs =
        persistentListOf(
            NotificationUM.Warning(
                title = stringReference("Send only XLM on the Ethereum network"),
                subtitle = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
            ),
        ),
        addresses = persistentListOf(
            address,
            address,
            address.copy(type = ReceiveAddress.Type.Ens, value = "papasha.eth"),
        ),
        showMemoDisclaimer = false,
        onCopyClick = {},
        onOpenQrCodeClick = {},
        isEnsResultLoading = true,
        network = "USDT",
        currencyIconState = CurrencyIconState.Locked,
        onShareClick = {},
    )

    override val values: Sequence<ReceiveAssetsUM>
        get() = sequenceOf(config)
}