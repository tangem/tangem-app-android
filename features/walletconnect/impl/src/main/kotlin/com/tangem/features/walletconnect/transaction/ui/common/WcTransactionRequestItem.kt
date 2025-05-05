package com.tangem.features.walletconnect.transaction.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcTransactionRequestItem(@DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing4)
                .weight(1f),
            text = stringResourceSafe(R.string.wc_transaction_request),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing12)
                .size(width = TangemTheme.dimens.size18, height = TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_chevron_right_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}