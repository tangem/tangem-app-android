package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WcConnectedAppInfoBS(state: WcConnectedAppInfoUM) {
    TangemModalBottomSheet<WcConnectedAppInfoUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.wc_wallet_connect),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.onDismiss,
            )
        },
        content = {
            WcConnectedAppInfoBSContent(
                state,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
            )
        },
    )
}

@Composable
private fun WcConnectedAppInfoBSContent(state: WcConnectedAppInfoUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val blocksModifier = Modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
        AppInfoFirstBlock(
            state = state,
            modifier = Modifier
                .padding(top = 8.dp)
                .then(blocksModifier),
        )
        state.notification?.let { notification ->
            val (containerColor, titleColor, iconTint) = when (notification) {
                WcAppInfoSecurityNotification.SecurityRisk -> Triple(
                    TangemColorPalette.Amaranth.copy(alpha = 0.1F),
                    TangemTheme.colors.text.warning,
                    TangemTheme.colors.icon.warning,
                )
                WcAppInfoSecurityNotification.UnknownDomain -> Triple(
                    null,
                    TangemTheme.colors.text.primary1,
                    TangemTheme.colors.icon.attention,
                )
            }
            Notification(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing14),
                config = notification.config,
                containerColor = containerColor,
                titleColor = titleColor,
                subtitleColor = TangemTheme.colors.text.primary1,
                iconTint = iconTint,
            )
        }
        NetworksBlock(
            networks = state.networks,
            modifier = Modifier
                .padding(top = 14.dp)
                .then(blocksModifier),
        )
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            text = stringResourceSafe(R.string.common_disconnect),
            enabled = state.disconnectButtonConfig.enabled,
            showProgress = state.disconnectButtonConfig.showProgress,
            onClick = state.disconnectButtonConfig.onClick,
        )
    }
}

@Composable
private fun AppInfoFirstBlock(state: WcConnectedAppInfoUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WcAppInfoItem(
            iconUrl = state.appIcon,
            title = state.appName,
            subtitle = state.appSubtitle,
            verifiedDAppState = state.verifiedDAppState,
        )
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors.stroke.primary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                text = state.walletName,
                textAlign = TextAlign.End,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun NetworksBlock(networks: ImmutableList<WcNetworkInfoItem.Required>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 4.dp)
                .padding(horizontal = 12.dp),
            text = stringResourceSafe(R.string.wc_connected_networks),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        networks.fastForEach { network ->
            key(network.id) { WcNetworkInfoItem(icon = network.icon, name = network.name, symbol = network.symbol) }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcConnectedAppInfoBS_Preview() {
    TangemThemePreview {
        WcConnectedAppInfoBS(
            state = WcConnectedAppInfoUM(
                appName = "React App",
                appIcon = "",
                verifiedDAppState = VerifiedDAppState.Verified {},
                isVerified = true,
                appSubtitle = "react-app.walletconnect.com",
                walletName = "Tangem 2.0",
                networks = persistentListOf(
                    WcNetworkInfoItem.Required(
                        id = "1",
                        icon = R.drawable.img_optimism_22,
                        name = "img_optimism_22img_optimism_22",
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
                        icon = R.drawable.img_solana_22,
                        name = "img_solana_22",
                        symbol = "solana",
                    ),
                ),
                disconnectButtonConfig = WcPrimaryButtonConfig(showProgress = false, enabled = true, onClick = {}),
                onDismiss = {},
            ),
        )
    }
}