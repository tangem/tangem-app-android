package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcNetworkFeeItem(networkFeeText: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_fee_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )

        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
            text = stringResourceSafe(R.string.common_network_fee_title),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )

        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing6)
                .size(TangemTheme.dimens.size16),
            painter = painterResource(id = R.drawable.ic_information_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = networkFeeText,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.tertiary,
            )
            Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing6))
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(width = TangemTheme.dimens.size18, height = TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_select_18_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
    }
}