package com.tangem.features.moneyaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.features.moneyaccount.main.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun MoneyAccountMainBlockContent(
    state: MoneyAccountMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is MoneyAccountMainUM.Empty -> Unit
        is MoneyAccountMainUM.Loading -> MoneyAccountMainLoading(modifier)
        is MoneyAccountMainUM.Onboarding -> MoneyAccountRow(
            subtitle = state.subtitle,
            modifier = modifier,
            onClick = state.onClick,
        )
        is MoneyAccountMainUM.ActionRequired -> MoneyAccountRow(
            subtitle = resourceReference(R.string.money_account_main_status_action_required),
            subtitleColor = TangemTheme.colors3.text.status.warning,
            balance = state.balance,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
            onClick = state.onClick,
        )
        is MoneyAccountMainUM.Content -> MoneyAccountRow(
            subtitle = state.subtitle,
            balance = state.balance,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
            onClick = state.onClick,
        )
        is MoneyAccountMainUM.Unavailable -> MoneyAccountUnavailable(modifier)
    }
}

@Composable
private fun MoneyAccountRow(
    subtitle: TextReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color = TangemTheme.colors3.text.secondary,
    balance: MoneyAccountMainUM.BalanceUM? = null,
    isBalanceHidden: Boolean = false,
) {
    TangemRow(
        modifier = modifier.moneyAccountCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onClick,
        startSlot = { MoneyAccountIcon() },
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.money_account_main_name),
                role = TangemRowTextRole.Title,
            )
        },
        subtitleSlot = {
            Text(
                text = subtitle.resolveReference(),
                color = subtitleColor,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        valueSlot = balance?.let {
            {
                MoneyAccountFiatAmount(
                    text = balance.amount.orMaskWithStars(isBalanceHidden).resolveReference(),
                    isBalanceFlickering = balance.isFlickering,
                    isBalanceFromCache = balance.isFromCache,
                )
            }
        },
        subvalueSlot = balance?.let {
            { TangemRowText(text = balance.currency, role = TangemRowTextRole.Subvalue) }
        },
    )
}

@Composable
private fun MoneyAccountUnavailable(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.moneyAccountCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { MoneyAccountIcon() },
        titleSlot = {
            Text(
                text = resourceReference(R.string.money_account_main_name).resolveReference(),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.body.medium,
            )
        },
        subtitleSlot = {
            Text(
                text = DASH_SIGN,
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.caption.medium,
            )
        },
    )
}

@Composable
private fun MoneyAccountIcon() {
    AccountIcon(
        name = TextReference.EMPTY,
        icon = AccountIconUM.Money,
        size = AccountIconSize.Default,
    )
}

@Composable
private fun MoneyAccountFiatAmount(text: String, isBalanceFlickering: Boolean, isBalanceFromCache: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isBalanceFromCache) {
            Icon(
                modifier = Modifier.size(12.dp),
                painter = painterResource(CoreUiR.drawable.ic_error_sync_24),
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
        }
        Text(
            text = text,
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

@Composable
private fun MoneyAccountMainLoading(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.moneyAccountCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { CircleShimmer(modifier = Modifier.size(40.dp)) },
        titleSlot = {
            TextShimmer(
                style = TangemTheme.typography3.body.medium,
                radius = 10.dp,
                modifier = Modifier.width(100.dp),
            )
        },
        subtitleSlot = {
            TextShimmer(
                style = TangemTheme.typography3.caption.medium,
                radius = 10.dp,
                modifier = Modifier.width(44.dp),
            )
        },
        valueSlot = {
            TextShimmer(
                style = TangemTheme.typography3.body.medium,
                radius = 10.dp,
                modifier = Modifier.width(80.dp),
            )
        },
    )
}

@Composable
private fun Modifier.moneyAccountCard(): Modifier = this
    .clip(RoundedCornerShape(24.dp))
    .background(TangemTheme.colors3.bg.secondary)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountMainBlockContent_Preview(
    @PreviewParameter(MoneyAccountMainBlockContentPreviewParameterProvider::class)
    state: MoneyAccountMainUM,
) {
    TangemThemePreviewRedesign {
        MoneyAccountMainBlockContent(
            state = state,
            isBalanceHidden = false,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}

private class MoneyAccountMainBlockContentPreviewParameterProvider :
    CollectionPreviewParameterProvider<MoneyAccountMainUM>(
        collection = listOf(
            MoneyAccountMainUM.Loading,
            MoneyAccountMainUM.Unavailable,
            MoneyAccountMainUM.Onboarding(subtitle = TextReference.Str("Activating…"), onClick = {}),
            MoneyAccountMainUM.Onboarding(subtitle = TextReference.Str("Verification in progress…"), onClick = {}),
            MoneyAccountMainUM.ActionRequired(balance = previewBalance, onClick = {}),
            MoneyAccountMainUM.Content(
                subtitle = TextReference.Str("Polygon network"),
                balance = previewBalance.copy(isFlickering = true, isFromCache = true),
                onClick = {},
            ),
        ),
    )

private val previewBalance = MoneyAccountMainUM.BalanceUM(
    amount = TextReference.Str("$0.00"),
    currency = TextReference.Str("USD"),
    isFlickering = false,
    isFromCache = false,
)
// endregion