package com.tangem.common.ui.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon

private val balanceContentSpacing = 4.dp
private val cacheIconSize = 12.dp
private val previewPadding = 16.dp

/**
 * Design-system v2 (DS3) account row: an account icon with title / subtitle on the leading side and
 * a fiat balance with an optional price change on the trailing side, plus an optional full-width
 * slot below the row.
 *
 * A thin, opinionated wrapper over [TangemRow] shared by every account-like row (joint accounts and,
 * later, crypto accounts). It does not draw its own container background — supply it via [modifier].
 *
 * @param title              account name shown as the row title.
 * @param subtitle           secondary line, e.g. token / member counts.
 * @param icon               account icon.
 * @param balance            fiat balance, already resolved and masked when the balance is hidden.
 * @param priceChange        price-change indicator under the balance; `null` hides it.
 * @param isBalanceFlickering `true` applies the loading blade-brush to the balance and price change.
 * @param isBalanceFromCache  `true` shows the stale-data icon next to the balance.
 * @param onClick            row click handler; `null` makes the row non-interactive.
 * @param extraBottom        optional full-width content rendered below the row.
 */
@Composable
fun AccountRow(
    title: TextReference,
    subtitle: TextReference,
    icon: AccountIconUM,
    balance: AnnotatedString,
    modifier: Modifier = Modifier,
    priceChange: TangemPriceChange.State? = null,
    isBalanceFlickering: Boolean = false,
    isBalanceFromCache: Boolean = false,
    onClick: (() -> Unit)? = null,
    extraBottom: (@Composable () -> Unit)? = null,
) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onClick,
        startSlot = {
            AccountIcon(
                name = title,
                icon = icon,
                size = AccountIconSize.Default,
            )
        },
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = subtitle, role = TangemRowTextRole.Subtitle) },
        valueSlot = {
            AccountBalance(
                balance = balance,
                isBalanceFlickering = isBalanceFlickering,
                isBalanceFromCache = isBalanceFromCache,
            )
        },
        subvalueSlot = priceChange?.let { state ->
            { TangemPriceChange(state = state, isFlickering = isBalanceFlickering) }
        },
        extraBottomSlot = extraBottom,
    )
}

@Composable
private fun AccountBalance(
    balance: AnnotatedString,
    isBalanceFlickering: Boolean,
    isBalanceFromCache: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(balanceContentSpacing),
    ) {
        if (isBalanceFromCache) {
            Icon(
                modifier = Modifier.size(cacheIconSize),
                painter = painterResource(R.drawable.ic_error_sync_24),
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
        }
        Text(
            text = balance,
            style = TangemTheme.typography3.body.medium.applyBladeBrush(
                isEnabled = isBalanceFlickering,
                textColor = TangemTheme.colors3.text.primary,
            ),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountRow_Preview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(previewPadding),
        ) {
            AccountRow(
                title = stringReference("Family savings"),
                subtitle = stringReference("4 tokens • 5 members"),
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Family,
                    color = CryptoPortfolioIcon.Color.CandyGrapeFizz,
                ),
                balance = AnnotatedString("$1,204.56"),
                priceChange = TangemPriceChange.State(
                    value = stringReference("2.08%"),
                    direction = TangemPriceChange.Direction.Up,
                ),
                isBalanceFromCache = true,
                onClick = {},
            )
        }
    }
}