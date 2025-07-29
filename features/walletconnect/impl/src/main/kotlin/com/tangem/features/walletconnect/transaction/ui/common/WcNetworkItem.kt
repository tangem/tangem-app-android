package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.tooltip.TangemTooltip
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM

@Composable
internal fun WcNetworkItem(networkInfo: WcNetworkInfoUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var isTooltipEnabled by remember { mutableStateOf(false) }
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_network_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
                text = stringResourceSafe(R.string.wc_common_network),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
            )
            TangemTooltip(
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
                text = networkInfo.name,
                enabled = isTooltipEnabled,
                content = { contentModifier ->
                    Text(
                        modifier = contentModifier,
                        text = networkInfo.name,
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { isTooltipEnabled = it.hasVisualOverflow },
                    )
                },
            )
        }
        Image(
            painter = painterResource(id = networkInfo.iconRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing6)
                .clip(CircleShape)
                .size(TangemTheme.dimens.size20),
        )
    }
}