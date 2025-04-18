package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcNetworksInfo
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WcAppInfoModalBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<WcAppInfoUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = { state ->
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.wc_wallet_connect),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.onDismiss,
            )
        },
        content = { state -> WcAppInfoModalBottomSheetContent(state) },
    )
}

@Composable
private fun WcAppInfoModalBottomSheetContent(state: WcAppInfoUM) {
    Column(modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)) {
        val blocksModifier = Modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
        WcAppInfoFirstBlock(modifier = blocksModifier, state = state)
        state.notificationUM?.config?.let { config ->
            Notification(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing14),
                config = config,
                iconTint = TangemTheme.colors.icon.attention,
            )
        }
        WcAppInfoSecondBlock(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing14)
                .then(blocksModifier),
            state = state,
        )
        WcAppInfoButtons(
            modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing16),
            onDismiss = state.onDismiss,
            onConnect = state.onConnect,
        )
    }
}

@Composable
private fun WcAppInfoFirstBlock(state: WcAppInfoUM, modifier: Modifier = Modifier) {
    var connectionRequestExpanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.animateContentSize()) {
        WcAppInfoItem(
            iconUrl = state.appIcon,
            title = state.appName,
            subtitle = state.appSubtitle,
            isVerified = state.isVerified,
        )
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors.stroke.primary)
        ConnectionRequestBlock(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { connectionRequestExpanded = !connectionRequestExpanded }
                .padding(TangemTheme.dimens.spacing12),
            expanded = connectionRequestExpanded,
        )
        if (connectionRequestExpanded) {
            ConnectionRequestDescription(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .padding(top = 8.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ConnectionRequestBlock(expanded: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.ic_connect_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing4)
                .weight(1f),
            text = stringResourceSafe(R.string.wc_connection_request),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing12)
                .size(width = 18.dp, height = 24.dp),
            painter = painterResource(if (expanded) R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun ConnectionRequestDescription(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResourceSafe(R.string.wc_connection_reqeust_would_like),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
        ConnectionRequestDescriptionRow(
            iconPainter = painterResource(R.drawable.ic_check_24),
            tint = TangemTheme.colors.icon.accent,
            text = stringResourceSafe(R.string.wc_connection_reqeust_can_view_balance),
        )
        ConnectionRequestDescriptionRow(
            iconPainter = painterResource(R.drawable.ic_check_24),
            tint = TangemTheme.colors.icon.accent,
            text = stringResourceSafe(R.string.wc_connection_reqeust_request_approval),
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResourceSafe(R.string.wc_connection_reqeust_will_not),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
        ConnectionRequestDescriptionRow(
            iconPainter = painterResource(R.drawable.ic_close_24),
            tint = TangemTheme.colors.icon.warning,
            text = stringResourceSafe(R.string.wc_connection_reqeust_cant_sign),
        )
    }
}

@Composable
private fun ConnectionRequestDescriptionRow(
    iconPainter: Painter,
    tint: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TangemTheme.dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = iconPainter,
            contentDescription = null,
            tint = tint,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Composable
private fun WcAppInfoSecondBlock(state: WcAppInfoUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val itemsModifier = Modifier
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing12)
        WalletRowItem(modifier = itemsModifier, walletName = state.walletName)
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors.stroke.primary)
        SelectNetworksBlock(
            networksInfo = state.networksInfo,
            modifier = itemsModifier,
        )
        when (state.networksInfo) {
            is WcNetworksInfo.ContainsAllRequiredNetworks -> Unit
            is WcNetworksInfo.MissingRequiredNetworkInfo -> {
                HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors.stroke.primary)
                Notification(
                    config = NotificationConfig(
                        iconResId = R.drawable.ic_alert_circle_24,
                        title = resourceReference(R.string.wc_missing_required_network_title),
                        subtitle = resourceReference(
                            R.string.wc_missing_required_network_description,
                            wrappedList(state.networksInfo.network),
                        ),
                    ),
                    iconTint = TangemTheme.colors.icon.attention,
                    containerColor = TangemTheme.colors.background.action,
                )
            }
        }
    }
}

@Composable
private fun WalletRowItem(walletName: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.ic_wallet_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing4)
                .weight(1f),
            text = stringResourceSafe(R.string.manage_tokens_network_selector_wallet),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing16)
                .weight(1f),
            text = walletName,
            textAlign = TextAlign.End,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.tertiary,
        )
        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing12)
                .size(width = 18.dp, height = 24.dp),
            painter = painterResource(R.drawable.ic_select_18_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun SelectNetworksBlock(networksInfo: WcNetworksInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.ic_network_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing4)
                .weight(1f),
            text = stringResourceSafe(R.string.wc_common_networks),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        when (networksInfo) {
            is WcNetworksInfo.ContainsAllRequiredNetworks -> NetworkIcons(icons = networksInfo.icons)
            is WcNetworksInfo.MissingRequiredNetworkInfo -> Unit
        }
        Icon(
            modifier = Modifier.size(width = 18.dp, height = 24.dp),
            painter = painterResource(
                id = when (networksInfo) {
                    is WcNetworksInfo.ContainsAllRequiredNetworks -> R.drawable.ic_select_18_24
                    is WcNetworksInfo.MissingRequiredNetworkInfo -> R.drawable.ic_chevron_right_24
                },
            ),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun NetworkIcons(icons: ImmutableList<Int>, modifier: Modifier = Modifier) {
    val (iconsToShow, remainingCount) = remember(icons) {
        when {
            icons.size <= 4 -> icons to 0
            else -> icons.take(3) to icons.size - 3
        }
    }
    Box(modifier = modifier.wrapContentWidth()) {
        iconsToShow.forEachIndexed { index, iconRes ->
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing16.times(index))
                    .border(
                        width = 2.dp,
                        color = TangemTheme.colors.background.action,
                        shape = CircleShape,
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .size(20.dp),
            )
        }
        if (remainingCount > 0) {
            Box(
                modifier = modifier
                    .padding(start = 48.dp)
                    .border(
                        width = 2.dp,
                        color = TangemTheme.colors.background.action,
                        shape = CircleShape,
                    )
                    .padding(2.dp)
                    .background(color = TangemTheme.colors.background.action)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = TangemTheme.colors.icon.primary1.copy(alpha = 0.1F)),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "+$remainingCount",
                    style = TangemTheme.typography.overline,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun WcAppInfoButtons(onDismiss: () -> Unit, onConnect: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
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
            onClick = onConnect,
        )
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcAppInfoBottomSheetPreview(@PreviewParameter(WcAppInfoStateProvider::class) state: WcAppInfoUM) {
    TangemThemePreview {
        WcAppInfoModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = state,
            ),
        )
    }
}

private class WcAppInfoStateProvider : CollectionPreviewParameterProvider<WcAppInfoUM>(
    collection = listOf(
        WcAppInfoUM(
            appName = "React App",
            appIcon = "",
            isVerified = true,
            appSubtitle = "react-app.walletconnect.com",
            notificationUM = null,
            walletName = "Tangem 2.0",
            networksInfo = WcNetworksInfo.ContainsAllRequiredNetworks(
                icons = persistentListOf(
                    R.drawable.img_optimism_22,
                    R.drawable.img_bsc_22,
                    R.drawable.img_avalanche_22,
                    R.drawable.img_solana_22,
                    R.drawable.img_avalanche_22,
                ),
            ),
            onDismiss = {},
            onConnect = {},
        ),
        WcAppInfoUM(
            appName = "React App",
            appIcon = "",
            isVerified = false,
            appSubtitle = "react-app.walletconnect.com",
            notificationUM = NotificationUM.Info(
                title = resourceReference(R.string.wc_alert_audit_unknown_domain),
                subtitle = resourceReference(R.string.wc_alert_domain_issues_description),
            ),
            walletName = "Tangem 2.0",
            networksInfo = WcNetworksInfo.MissingRequiredNetworkInfo(network = "Solana"),
            onDismiss = {},
            onConnect = {},
        ),
    ),
)