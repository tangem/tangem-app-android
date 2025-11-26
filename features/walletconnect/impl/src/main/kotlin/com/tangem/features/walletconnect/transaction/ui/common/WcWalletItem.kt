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
import com.tangem.common.ui.account.AccountTitle
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.tooltip.TangemTooltip
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcPortfolioItem(portfolioName: AccountTitleUM, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        var isTooltipEnabled by remember { mutableStateOf(false) }
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(R.drawable.ic_wallet_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        val leftText: String = when (portfolioName) {
            is AccountTitleUM.Account -> stringResourceSafe(R.string.common_account)
            is AccountTitleUM.Text -> stringResourceSafe(R.string.wc_common_wallet)
        }
        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
            text = leftText,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
        )
        SpacerWMax()
        when (portfolioName) {
            is AccountTitleUM.Account -> AccountTitle(
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
                accountTitleUM = portfolioName,
                textStyle = TangemTheme.typography.body1,
                textColor = TangemTheme.colors.text.tertiary,
                iconSize = AccountIconSize.Small,
            )
            is AccountTitleUM.Text -> TangemTooltip(
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
                text = portfolioName.title.resolveReference(),
                enabled = isTooltipEnabled,
                content = { contentModifier ->
                    Text(
                        modifier = contentModifier,
                        text = portfolioName.title.resolveReference(),
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
}