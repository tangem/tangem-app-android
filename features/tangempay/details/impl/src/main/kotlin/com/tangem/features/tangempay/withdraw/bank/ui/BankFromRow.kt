package com.tangem.features.tangempay.withdraw.bank.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFromUM

/**
 * "From · <source account>" header shared by the amount and confirm steps; a non-null [balance] renders
 * trailing, right-aligned.
 */
@Composable
internal fun BankFromRow(from: BankWithdrawFromUM, balance: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResourceSafe(R.string.common_from),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        AccountIcon(name = from.title, icon = from.icon, size = AccountIconSize.ExtraSmall)
        Text(
            modifier = if (balance != null) Modifier.weight(1f) else Modifier,
            text = from.title.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        if (balance != null) {
            Text(
                text = balance,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.tertiary,
            )
        }
    }
}