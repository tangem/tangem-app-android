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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme
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
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size48)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius8)),
            model = iconUrl,
            contentDescription = title,
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
                    modifier = Modifier.weight(1f, fill = false),
                    text = title,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.h3,
                )
                if (verifiedDAppState is VerifiedDAppState.Verified) {
                    Icon(
                        modifier = Modifier.size(20.dp),
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
            )
        }
    }
}

@Composable
internal fun WcNetworkInfoItem(@DrawableRes icon: Int, name: String, symbol: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 14.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Text(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f, fill = false),
            text = name,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = symbol,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}