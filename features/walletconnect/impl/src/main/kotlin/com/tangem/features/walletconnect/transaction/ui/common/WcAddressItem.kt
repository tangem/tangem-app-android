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
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.tooltip.TangemTooltip
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcAddressItem(address: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        var isTooltipEnabled by remember { mutableStateOf(false) }
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_user_square_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
            text = stringResourceSafe(R.string.wc_common_address),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
        )
        SpacerWMax()
        TangemTooltip(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
            text = address,
            enabled = isTooltipEnabled,
            content = { contentModifier ->
                EllipsisText(
                    onTextLayout = { isTooltipEnabled = !it.hasVisualOverflow },
                    modifier = contentModifier,
                    text = address,
                    textAlign = TextAlign.End,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                    ellipsis = TextEllipsis.Middle,
                )
            },
        )
    }
}