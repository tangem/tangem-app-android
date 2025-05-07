package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcWalletItem(walletName: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_wallet_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing8)
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
    }
}