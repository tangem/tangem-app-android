package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.tooltip.TangemTooltip
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcWalletItem(walletName: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        var isTooltipEnabled by remember { mutableStateOf(false) }
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_wallet_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
            text = stringResourceSafe(R.string.manage_tokens_network_selector_wallet),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
        )
        SpacerWMax()
        TangemTooltip(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
            text = walletName,
            enabled = isTooltipEnabled,
            content = { contentModifier ->
                Text(
                    modifier = contentModifier,
                    text = walletName,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { isTooltipEnabled = it.hasVisualOverflow },
                )
            },
        )
    }
}