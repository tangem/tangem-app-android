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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.assets.il_account_safe
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.test.MoneyAccountTestTags
import com.tangem.core.ui.utils.dashedBorder
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.features.moneyaccount.main.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun MoneyAccountMainBlockContent(
    state: MoneyAccountMainUM,
    isBalanceHidden: Boolean,
    isGrouped: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is MoneyAccountMainUM.Empty -> Unit
        is MoneyAccountMainUM.NotStarted -> if (isGrouped) {
            MoneyAccountNotStartedRow(onDetailsClick = state.onDetailsClick, modifier = modifier)
        }
        is MoneyAccountMainUM.Onboarding -> MoneyAccountRow(
            isGrouped = isGrouped,
            subtitle = state.subtitle,
            modifier = modifier,
            onClick = state.onClick,
        )
        is MoneyAccountMainUM.Failed -> MoneyAccountRow(
            isGrouped = isGrouped,
            subtitle = state.subtitle,
            modifier = modifier,
            onClick = state.onClick,
            subtitleColor = TangemTheme.colors3.text.status.error,
        )
        is MoneyAccountMainUM.Content -> MoneyAccountRow(
            isGrouped = isGrouped,
            subtitle = state.subtitle,
            balance = state.balance,
            isBalanceHidden = isBalanceHidden,
            isActionRequired = state.isActionRequired,
            modifier = modifier,
            onClick = state.onClick,
        )
        is MoneyAccountMainUM.Unavailable -> MoneyAccountUnavailable(isGrouped = isGrouped, modifier = modifier)
    }
}

@Composable
private fun MoneyAccountRow(
    isGrouped: Boolean,
    subtitle: TextReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    balance: MoneyAccountMainUM.BalanceUM? = null,
    isBalanceHidden: Boolean = false,
    isActionRequired: Boolean = false,
    subtitleColor: Color = TangemTheme.colors3.text.secondary,
) {
    Box(modifier = modifier) {
        TangemRow(
            modifier = Modifier
                .moneyAccountCard(
                    isActionRequired = isActionRequired,
                    isGrouped = isGrouped,
                )
                .testTag(MoneyAccountTestTags.MAIN_ROW),
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
            subtitleSlot = if (!subtitle.isNullOrEmpty()) {
                {
                    Text(
                        text = subtitle.resolveReference(),
                        color = if (isActionRequired) TangemTheme.colors3.text.status.warning else subtitleColor,
                        style = TangemTheme.typography3.caption.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                null
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
        if (isActionRequired && !isGrouped) {
            TangemGlowRing(
                modifier = Modifier.matchParentSize(),
                variant = TangemGlowRing.Variant.Warning,
                cornerRadius = 24.dp,
            )
        }
    }
}

@Composable
private fun MoneyAccountUnavailable(isGrouped: Boolean, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier
            .moneyAccountCard(
                isActionRequired = false,
                isGrouped = isGrouped,
            )
            .testTag(MoneyAccountTestTags.MAIN_ROW),
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
private fun MoneyAccountNotStartedRow(onDetailsClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.testTag(MoneyAccountTestTags.MAIN_ROW),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onDetailsClick,
        startSlot = { MoneyAccountPlaceholderIcon() },
        titleSlot = {
            Text(
                text = resourceReference(R.string.money_account_main_name).resolveReference(),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitleSlot = {
            Text(
                text = resourceReference(R.string.money_account_main_status_active_v2).resolveReference(),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        endSlot = {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                text = resourceReference(R.string.money_account_main_details_button),
                onClick = onDetailsClick,
            )
        },
    )
}

@Composable
private fun MoneyAccountPlaceholderIcon() {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .dashedBorder(
                color = TangemTheme.colors3.border.tertiary,
                shape = shape,
                dashLength = 4.dp,
                gapLength = 4.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            imageVector = Icons.il_account_safe,
            tint = TangemTheme.colors3.icon.tertiary,
            contentDescription = null,
        )
    }
}

@Composable
private fun MoneyAccountIcon() {
    TangemAvatar(
        state = TangemAvatar.UiState.Account(kind = TangemAvatar.AccountKind.Money),
        size = TangemAvatar.Size.X40,
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
                modifier = Modifier
                    .size(12.dp)
                    .testTag(MoneyAccountTestTags.MAIN_ROW_CACHE_ICON),
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
private fun Modifier.moneyAccountCard(isActionRequired: Boolean, isGrouped: Boolean): Modifier = if (isGrouped) {
    this
} else {
    clip(RoundedCornerShape(24.dp))
        .background(
            if (isActionRequired) TangemTheme.colors3.bg.status.warningSubtle else TangemTheme.colors3.bg.secondary,
        )
}

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
            isGrouped = false,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountMainBlockContent_NotStarted_Preview() {
    TangemThemePreviewRedesign {
        MoneyAccountMainBlockContent(
            state = MoneyAccountMainUM.NotStarted(onDetailsClick = {}),
            isBalanceHidden = false,
            isGrouped = true,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(16.dp),
        )
    }
}

private class MoneyAccountMainBlockContentPreviewParameterProvider :
    CollectionPreviewParameterProvider<MoneyAccountMainUM>(
        collection = listOf(
            MoneyAccountMainUM.Unavailable,
            MoneyAccountMainUM.Onboarding(subtitle = TextReference.Str("Activating…"), onClick = {}),
            MoneyAccountMainUM.Failed(subtitle = TextReference.Str("Failed"), onClick = {}),
            MoneyAccountMainUM.Content(
                subtitle = TextReference.Str("Polygon network"),
                balance = previewBalance.copy(isFlickering = true, isFromCache = true),
                isActionRequired = false,
                onClick = {},
            ),
            MoneyAccountMainUM.Content(
                subtitle = TextReference.EMPTY,
                balance = null,
                isActionRequired = false,
                onClick = {},
            ),
            MoneyAccountMainUM.Content(
                subtitle = resourceReference(R.string.tangempay_action_required_title),
                balance = previewBalance,
                isActionRequired = true,
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