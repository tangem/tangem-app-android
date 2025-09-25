package com.tangem.features.walletconnect.connections.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.WalletConnectBottomSheetTestTags
import com.tangem.core.ui.test.WalletConnectDetailsBottomSheetTestTags
import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcAppInfoItem(
    iconUrl: String,
    title: String,
    subtitle: String,
    verifiedDAppState: VerifiedDAppState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .conditional(
                condition = verifiedDAppState is VerifiedDAppState.Verified,
                modifier = {
                    clickableSingle { (verifiedDAppState as VerifiedDAppState.Verified).onVerifiedClick() }
                },
            )
            .padding(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size48)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius8))
                .testTag(WalletConnectBottomSheetTestTags.APP_ICON),
            model = iconUrl,
            contentDescription = title,
            error = painterResource(R.drawable.img_wc_dapp_icon_placeholder_48),
            fallback = painterResource(R.drawable.img_wc_dapp_icon_placeholder_48),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag(WalletConnectBottomSheetTestTags.APP_NAME),
                    text = title,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.h3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (verifiedDAppState is VerifiedDAppState.Verified) {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag(WalletConnectBottomSheetTestTags.APPROVE_ICON),
                        painter = painterResource(R.drawable.img_approvale2_20),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                }
            }
            Text(
                text = subtitle,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                modifier = Modifier.testTag(WalletConnectBottomSheetTestTags.APP_URL),
            )
        }
    }
}

@Composable
internal fun WcNetworkInfoItem(@DrawableRes icon: Int, name: String, symbol: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(vertical = 14.dp, horizontal = 12.dp)
            .testTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_ITEM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .testTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_ICON),
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Text(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f, fill = false)
                .testTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_NAME),
            text = name,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .testTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_SYMBOL),
            text = symbol,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}