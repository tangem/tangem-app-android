package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_network_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing8)
                .weight(1f),
            text = stringResourceSafe(R.string.wc_common_network),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = networkInfo.name,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.tertiary,
            )
            Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing6))
            Image(
                painter = painterResource(id = networkInfo.iconRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(TangemTheme.dimens.size20),
            )
        }
    }
}