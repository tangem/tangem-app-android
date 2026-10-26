package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.PortfolioSelectRow
import com.tangem.common.ui.account.PortfolioSelectRowPreviewData
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.WalletConnectBottomSheetTestTags
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WcAppInfoModalBottomSheet(state: WcAppInfoUM, onBack: () -> Unit, onDismiss: () -> Unit) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors3.bg.primary,
        onBack = onBack,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.wc_wallet_connect),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onDismiss,
                modifier = Modifier.testTag(WalletConnectBottomSheetTestTags.TITLE),
            )
        },
        content = {
            WcAppInfoModalBottomSheet(
                modifier = Modifier.padding(horizontal = 16.dp),
                state = state,
            )
        },
        footer = {
            WcAppInfoButtons(
                modifier = Modifier.padding(16.dp),
                onDismiss = onDismiss,
                connectButtonConfig = state.connectButtonConfig,
            )
        },
    )
}

@Composable
private fun WcAppInfoModalBottomSheet(state: WcAppInfoUM, modifier: Modifier = Modifier) {
    when (state) {
        is WcAppInfoUM.Content -> WcAppInfoModalBottomSheetContent(state, modifier)
        is WcAppInfoUM.Loading -> WcAppInfoModalBottomSheetLoading(state, modifier)
    }
}

// region Content state
@Composable
private fun WcAppInfoModalBottomSheetContent(state: WcAppInfoUM.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val blocksModifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color = TangemTheme.colors3.bg.secondary)
            .fillMaxWidth()
        WcAppInfoFirstBlock(modifier = blocksModifier, state = state)
        state.notification?.let { notification ->
            Notification(
                modifier = Modifier.padding(top = 14.dp),
                config = notification.config,
                containerColor = when (notification) {
                    WcAppInfoSecurityNotification.SecurityRisk -> TangemColorPalette.Amaranth.copy(alpha = 0.1F)
                    WcAppInfoSecurityNotification.UnknownDomain -> null
                },
                titleColor = when (notification) {
                    WcAppInfoSecurityNotification.SecurityRisk -> TangemTheme.colors3.text.accent.red
                    WcAppInfoSecurityNotification.UnknownDomain -> TangemTheme.colors3.text.primary
                },
                subtitleColor = TangemTheme.colors3.text.primary,
                iconTint = when (notification) {
                    WcAppInfoSecurityNotification.SecurityRisk -> TangemTheme.colors3.icon.accent.red
                    WcAppInfoSecurityNotification.UnknownDomain -> TangemTheme.colors3.icon.accent.yellow
                },
            )
        }
        WcAppInfoSecondBlock(
            modifier = Modifier
                .padding(top = 14.dp)
                .then(blocksModifier),
            state = state,
        )
    }
}

@Composable
private fun WcAppInfoFirstBlock(state: WcAppInfoUM.Content, modifier: Modifier = Modifier) {
    var isConnectionRequestExpanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        WcAppInfoItem(
            iconUrl = state.appIcon,
            title = state.appName,
            subtitle = state.appSubtitle,
            verifiedDAppState = state.verifiedDAppState,
        )
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
        ConnectionRequestBlock(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isConnectionRequestExpanded = !isConnectionRequestExpanded }
                .padding(12.dp),
            expanded = isConnectionRequestExpanded,
        )
        if (isConnectionRequestExpanded) {
            ConnectionRequestDescription(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ConnectionRequestBlock(expanded: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .testTag(WalletConnectBottomSheetTestTags.CONNECTION_REQUEST_ICON),
            painter = painterResource(R.drawable.ic_connect_new_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.accent.blue,
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
                .testTag(WalletConnectBottomSheetTestTags.CONNECTION_REQUEST_TEXT),
            text = stringResourceSafe(R.string.wc_connection_request),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors3.text.primary,
        )
        Icon(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(width = 18.dp, height = 24.dp)
                .testTag(WalletConnectBottomSheetTestTags.CONNECTION_REQUEST_CHEVRON),
            painter = painterResource(if (expanded) R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
        )
    }
}

@Composable
private fun ConnectionRequestDescription(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResourceSafe(R.string.wc_connection_reqeust_would_like),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors3.text.secondary,
        )
        ConnectionRequestDescriptionRow(
            iconPainter = painterResource(R.drawable.ic_check_24),
            tint = TangemTheme.colors3.icon.accent.blue,
            text = stringResourceSafe(R.string.wc_connection_reqeust_can_view_balance),
            backgroundColor = TangemTheme.colors3.icon.accent.blue,
        )
        ConnectionRequestDescriptionRow(
            iconPainter = painterResource(R.drawable.ic_check_24),
            tint = TangemTheme.colors3.icon.accent.blue,
            text = stringResourceSafe(R.string.wc_connection_reqeust_request_approval),
            backgroundColor = TangemTheme.colors3.icon.accent.blue,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            thickness = 0.5.dp,
            color = TangemTheme.colors3.border.secondary,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResourceSafe(R.string.wc_connection_reqeust_will_not),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors3.text.secondary,
        )
        ConnectionRequestDescriptionRow(
            iconPainter = painterResource(R.drawable.ic_close_24),
            tint = TangemTheme.colors3.icon.accent.red,
            text = stringResourceSafe(R.string.wc_connection_reqeust_cant_sign),
            backgroundColor = TangemTheme.colors3.icon.accent.red,
        )
    }
}

@Composable
private fun ConnectionRequestDescriptionRow(
    iconPainter: Painter,
    backgroundColor: Color,
    tint: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(backgroundColor.copy(alpha = 0.1F)),
            contentAlignment = Alignment.Center,
            content = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = iconPainter,
                    contentDescription = null,
                    tint = tint,
                )
            },
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun WcAppInfoSecondBlock(state: WcAppInfoUM.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val itemsModifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
        PortfolioRowItem(portfolioSelectRow = state.portfolioSelectRow)
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
        SelectNetworksBlock(
            modifier = Modifier
                .clickable(onClick = state.onNetworksClick)
                .then(itemsModifier),
            networksInfo = state.networksInfo,
        )
        when (state.networksInfo) {
            is WcNetworksInfo.ContainsAllRequiredNetworks -> Unit
            is WcNetworksInfo.NoneNetworksAdded -> {
                HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
                Notification(
                    config = NotificationConfig(
                        iconResId = R.drawable.ic_alert_circle_24,
                        title = resourceReference(R.string.wc_specify_networks_title),
                        subtitle = resourceReference(R.string.wc_specify_networks_subtitle),
                    ),
                    iconTint = TangemTheme.colors3.icon.accent.yellow,
                    containerColor = TangemTheme.colors3.bg.secondary,
                )
            }
            is WcNetworksInfo.MissingRequiredNetworkInfo -> {
                HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
                Notification(
                    config = NotificationConfig(
                        iconResId = R.drawable.ic_alert_circle_24,
                        title = resourceReference(R.string.wc_missing_required_network_title),
                        subtitle = resourceReference(
                            R.string.wc_missing_required_network_description,
                            wrappedList(state.networksInfo.networks),
                        ),
                    ),
                    iconTint = TangemTheme.colors3.icon.accent.yellow,
                    containerColor = TangemTheme.colors3.bg.secondary,
                )
            }
        }
    }
}

@Composable
private fun PortfolioRowItem(portfolioSelectRow: PortfolioSelectUM, modifier: Modifier = Modifier) {
    PortfolioSelectRow(modifier = modifier, state = portfolioSelectRow) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .testTag(WalletConnectBottomSheetTestTags.WALLET_ICON),
            painter = painterResource(R.drawable.ic_wallet_new_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.accent.blue,
        )
        SpacerW4()
    }
}

@Composable
private fun SelectNetworksBlock(networksInfo: WcNetworksInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .testTag(WalletConnectBottomSheetTestTags.NETWORKS_ICON),
            painter = painterResource(R.drawable.ic_network_new_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.accent.blue,
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
                .testTag(WalletConnectBottomSheetTestTags.NETWORKS_TITLE),
            text = stringResourceSafe(R.string.wc_common_networks),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors3.text.primary,
        )
        when (networksInfo) {
            is WcNetworksInfo.ContainsAllRequiredNetworks -> NetworkIcons(items = networksInfo.items)
            is WcNetworksInfo.MissingRequiredNetworkInfo -> Unit
            is WcNetworksInfo.NoneNetworksAdded -> Unit
        }
        Icon(
            modifier = Modifier
                .size(width = 18.dp, height = 24.dp)
                .testTag(WalletConnectBottomSheetTestTags.NETWORKS_SELECTOR_ICON),
            painter = painterResource(id = R.drawable.ic_select_18_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun NetworkIcons(items: ImmutableList<WcNetworkInfoItem>, modifier: Modifier = Modifier) {
    val (iconsToShow, remainingCount) = remember(items) {
        when {
            items.size <= 4 -> items to 0
            else -> items.take(3) to items.size - 3
        }
    }
    Box(modifier = modifier.wrapContentWidth()) {
        iconsToShow.forEachIndexed { index, item ->
            Image(
                painter = painterResource(id = item.icon),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 16.dp.times(index))
                    .border(
                        width = 2.dp,
                        color = TangemTheme.colors3.bg.secondary,
                        shape = CircleShape,
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .size(20.dp)
                    .testTag(WalletConnectBottomSheetTestTags.NETWORKS_ICONS),
            )
        }
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 48.dp)
                    .border(
                        width = 2.dp,
                        color = TangemTheme.colors3.bg.secondary,
                        shape = CircleShape,
                    )
                    .padding(2.dp)
                    .background(color = TangemTheme.colors3.bg.secondary)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = TangemTheme.colors3.icon.primary.copy(alpha = 0.1F)),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "+$remainingCount",
                    style = TangemTheme.typography.overline,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun WcAppInfoButtons(
    onDismiss: () -> Unit,
    connectButtonConfig: WcPrimaryButtonConfig,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = stringResourceSafe(R.string.common_cancel),
            onClick = onDismiss,
        )
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = stringResourceSafe(R.string.wc_common_connect),
            onClick = connectButtonConfig.onClick,
            showProgress = connectButtonConfig.shouldShowProgress,
            enabled = connectButtonConfig.isEnabled,
        )
    }
}
// endregion

// region Loading state
@Composable
private fun WcAppInfoModalBottomSheetLoading(state: WcAppInfoUM.Loading, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val blocksModifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color = TangemTheme.colors3.bg.secondary)
            .fillMaxWidth()
        WcAppInfoFirstLoadingBlock(blocksModifier)
        WcAppInfoSecondLoadingBlock(
            portfolioName = state.portfolioName,
            modifier = Modifier
                .padding(top = 14.dp)
                .then(blocksModifier),
        )
    }
}

@Composable
private fun WcAppInfoFirstLoadingBlock(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WcLoadingAppInfoItem()
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
        Row(
            modifier = Modifier.padding(all = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TangemTheme.colors3.icon.accent.blue,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Square,
            )
            Text(
                text = "Connecting...",
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}

@Composable
private fun WcAppInfoSecondLoadingBlock(portfolioName: TextReference, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(modifier = Modifier.padding(all = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_wallet_new_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.accent.blue,
            )
            Text(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(1f),
                text = portfolioName.resolveReference(),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors3.text.primary,
            )
            TextShimmer(style = TangemTheme.typography.body1, radius = 8.dp, text = "Wallet 2.0")
        }
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
        Row(modifier = Modifier.padding(all = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_network_new_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.accent.blue,
            )
            Text(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(1f),
                text = stringResourceSafe(R.string.wc_common_networks),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors3.text.primary,
            )
            WcAppInfoLoadingNetworkIcons()
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun WcAppInfoLoadingNetworkIcons(modifier: Modifier = Modifier) {
    Box(modifier = modifier.wrapContentWidth()) {
        for (index in 0..3) {
            CircleShimmer(
                modifier = Modifier
                    .padding(start = 16.dp.times(index))
                    .border(
                        width = 2.dp,
                        color = TangemTheme.colors3.bg.secondary,
                        shape = CircleShape,
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun WcLoadingAppInfoItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RectangleShimmer(modifier = Modifier.size(48.dp), radius = 16.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextShimmer(
                modifier = Modifier
                    .width(106.dp)
                    .height(24.dp),
                style = TangemTheme.typography.h3,
                radius = 8.dp,
            )
            TextShimmer(
                modifier = Modifier
                    .width(168.dp)
                    .height(20.dp),
                style = TangemTheme.typography.body2,
                radius = 8.dp,
            )
        }
    }
}
// endregion

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcAppInfoBottomSheetPreview(@PreviewParameter(WcAppInfoStateProvider::class) state: WcAppInfoUM.Content) {
    TangemThemePreview {
        WcAppInfoModalBottomSheet(
            state = state,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(horizontal = 16.dp),
        )
    }
}

private class WcAppInfoStateProvider : CollectionPreviewParameterProvider<WcAppInfoUM>(
    collection = listOf(
        WcAppInfoUM.Loading(
            onDismiss = {},
            portfolioName = stringReference("Wallet"),
            connectButtonConfig = WcPrimaryButtonConfig(shouldShowProgress = false, isEnabled = false, onClick = {}),
        ),
        WcAppInfoUM.Content(
            appName = "React App",
            appIcon = "",
            verifiedDAppState = VerifiedDAppState.Verified {},
            appSubtitle = "react-app.walletconnect.com",
            notification = WcAppInfoSecurityNotification.SecurityRisk,
            portfolioSelectRow = PortfolioSelectRowPreviewData.account,
            networksInfo = WcNetworksInfo.ContainsAllRequiredNetworks(
                items = persistentListOf(
                    WcNetworkInfoItem.Required(
                        id = "1",
                        icon = R.drawable.img_optimism_22,
                        name = "img_optimism_22",
                        symbol = "optimism",
                    ),
                    WcNetworkInfoItem.Required(
                        id = "2",
                        icon = R.drawable.img_bsc_22,
                        name = "img_bsc_22",
                        symbol = "bsc",
                    ),
                    WcNetworkInfoItem.Required(
                        id = "3",
                        icon = R.drawable.img_avalanche_22,
                        name = "img_avalanche_22",
                        symbol = "avalanche",
                    ),
                    WcNetworkInfoItem.Required(
                        id = "4",
                        icon = R.drawable.img_solana_22,
                        name = "img_solana_22",
                        symbol = "solana",
                    ),
                    WcNetworkInfoItem.Required(
                        id = "5",
                        icon = R.drawable.img_avalanche_22,
                        name = "img_avalanche_22",
                        symbol = "avalanche",
                    ),
                ),
            ),
            onNetworksClick = {},
            onDismiss = {},
            connectButtonConfig = WcPrimaryButtonConfig(shouldShowProgress = false, isEnabled = true, onClick = {}),
        ),
        WcAppInfoUM.Content(
            appName = "React App",
            appIcon = "",
            verifiedDAppState = VerifiedDAppState.Unknown,
            appSubtitle = "react-app.walletconnect.com",
            notification = WcAppInfoSecurityNotification.UnknownDomain,
            portfolioSelectRow = PortfolioSelectRowPreviewData.wallet,
            networksInfo = WcNetworksInfo.MissingRequiredNetworkInfo(networks = "Solana"),
            onNetworksClick = {},
            onDismiss = {},
            connectButtonConfig = WcPrimaryButtonConfig(shouldShowProgress = false, isEnabled = true, onClick = {}),
        ),
        WcAppInfoUM.Content(
            appName = "React App",
            appIcon = "",
            verifiedDAppState = VerifiedDAppState.Unknown,
            appSubtitle = "react-app.walletconnect.com",
            notification = WcAppInfoSecurityNotification.UnknownDomain,
            portfolioSelectRow = PortfolioSelectRowPreviewData.account,
            networksInfo = WcNetworksInfo.MissingRequiredNetworkInfo(networks = "Solana"),
            onNetworksClick = {},
            onDismiss = {},
            connectButtonConfig = WcPrimaryButtonConfig(shouldShowProgress = false, isEnabled = true, onClick = {}),
        ),
    ),
)