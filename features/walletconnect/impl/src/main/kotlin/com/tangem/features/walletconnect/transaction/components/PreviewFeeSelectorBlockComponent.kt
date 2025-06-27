package com.tangem.features.walletconnect.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.walletconnect.impl.R

internal class PreviewFeeSelectorBlockComponent : FeeSelectorBlockComponent {
    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier
                .background(TangemTheme.colors.background.action)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_fee_new_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
            Text(
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                text = stringResourceSafe(R.string.common_network_fee_title),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
            )
            Icon(
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing6)
                    .size(TangemTheme.dimens.size16),
                painter = painterResource(id = R.drawable.ic_token_info_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "~ 0.25 \$",
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.tertiary,
            )
            Icon(
                modifier = Modifier.size(width = 18.dp, height = 24.dp),
                painter = painterResource(id = R.drawable.ic_select_18_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}