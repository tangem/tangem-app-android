package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.PillKind
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.PillSubtitle
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TransactionStatusPill(
    state: TransactionItemUM.Pill,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = state.onClick)
            .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x2),
        horizontalArrangement = Arrangement.Center,
    ) {
        Pill(state = state, isBalanceHidden = isBalanceHidden)
    }
}

@Composable
private fun Pill(state: TransactionItemUM.Pill, isBalanceHidden: Boolean) {
    val labelColor = state.status.labelColor()
    val secondaryColor = state.status.secondaryColor()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(TangemTheme.colors2.tabs.backgroundSecondary)
            .padding(horizontal = TangemTheme.dimens2.x2, vertical = TangemTheme.dimens2.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        LeadingIcon(kind = state.kind, status = state.status)
        if (state.status is Status.Failed && state.amount != null) {
            Text(
                text = stringResourceSafe(R.string.common_action_failed, state.failedBody(isBalanceHidden)),
                color = labelColor,
                style = TangemTheme.typography2.captionMedium12,
            )
        } else {
            Text(
                text = state.label.resolveReference(),
                color = labelColor,
                style = TangemTheme.typography2.captionMedium12,
            )
            if (state.amount != null) {
                Text(
                    text = state.amount.orMaskWithStars(isBalanceHidden),
                    color = secondaryColor,
                    style = TangemTheme.typography2.captionMedium12,
                )
                state.currencySymbol?.let { symbol ->
                    Text(
                        text = symbol,
                        color = secondaryColor,
                        style = TangemTheme.typography2.captionMedium12,
                    )
                }
            }
        }
        val subtitle = state.subtitle
        if (subtitle is PillSubtitle.Address && state.status !is Status.Failed) {
            InlineImageSubtitle(
                template = stringResourceSafe(
                    R.string.transaction_history_to_inline_address,
                    subtitle.briefAddress,
                ),
                color = secondaryColor,
                afterIconColor = labelColor,
            ) {
                IdentIcon(
                    address = subtitle.rawAddress,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
        }
    }
}

@Composable
private fun LeadingIcon(kind: PillKind, status: Status) {
    if (status is Status.Unconfirmed) {
        CircularProgressIndicator(
            strokeWidth = 1.5.dp,
            color = TangemTheme.colors2.markers.iconBlue,
            modifier = Modifier.size(TangemTheme.dimens2.x4),
        )
        return
    }
    val iconRes = when (status) {
        is Status.Failed -> R.drawable.ic_close_24
        is Status.Confirmed -> when (kind) {
            PillKind.STAKING -> R.drawable.ic_transaction_history_staking_24
            PillKind.YIELD_MODE -> R.drawable.ic_yield_mode_16
            PillKind.APPROVE -> null
        }
        is Status.Unconfirmed -> null
    } ?: return
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = status.iconTint(),
        modifier = Modifier.size(TangemTheme.dimens2.x4),
    )
}

@Composable
private fun Status.labelColor(): Color = when (this) {
    is Status.Confirmed -> TangemTheme.colors2.text.neutral.secondary
    is Status.Unconfirmed -> TangemTheme.colors2.text.status.accent
    is Status.Failed -> TangemTheme.colors2.text.status.warning
}

@Composable
private fun Status.secondaryColor(): Color = when (this) {
    is Status.Confirmed -> TangemTheme.colors2.text.neutral.primary
    is Status.Unconfirmed -> TangemTheme.colors2.text.status.accent
    is Status.Failed -> TangemTheme.colors2.text.status.warning
}

@Composable
private fun Status.iconTint(): Color = when (this) {
    is Status.Confirmed -> TangemTheme.colors2.fill.neutral.primary
    is Status.Unconfirmed -> TangemTheme.colors2.markers.iconBlue
    is Status.Failed -> TangemTheme.colors2.markers.iconRed
}

@Composable
private fun TransactionItemUM.Pill.failedBody(isBalanceHidden: Boolean): String = buildString {
    append(label.resolveReference())
    amount?.let { value ->
        append(' ')
        append(value.orMaskWithStars(isBalanceHidden))
    }
    currencySymbol?.let { symbol ->
        append(' ')
        append(symbol)
    }
}

// region Preview

private fun previewPill(
    txHash: String,
    kind: PillKind,
    status: Status,
    label: String,
    amount: String? = null,
    currencySymbol: String? = null,
    subtitle: PillSubtitle? = null,
): TransactionItemUM.Pill = TransactionItemUM.Pill(
    txHash = txHash,
    kind = kind,
    status = status,
    label = stringReference(label),
    amount = amount,
    currencySymbol = currencySymbol,
    subtitle = subtitle,
    timestamp = 0L,
    onClick = {},
)

@Composable
private fun PillPreviewColumn(items: List<TransactionItemUM.Pill>) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors2.surface.level1)
            .padding(vertical = TangemTheme.dimens2.x2),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        items.forEach { TransactionStatusPill(state = it, isBalanceHidden = false) }
    }
}

@Suppress("NamedArguments")
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionStatusPill_Staking() {
    TangemThemePreviewRedesign {
        PillPreviewColumn(
            items = listOf(
                previewPill("stk-c", PillKind.STAKING, Status.Confirmed, "Staked", "950.43", "TRX"),
                previewPill("stk-u", PillKind.STAKING, Status.Unconfirmed, "Staking", "1,000.00", "TRX"),
                previewPill("stk-f", PillKind.STAKING, Status.Failed, "Staking failed"),
                previewPill("ust-c", PillKind.STAKING, Status.Confirmed, "Unstaked", "950.43", "TRX"),
                previewPill("ust-u", PillKind.STAKING, Status.Unconfirmed, "Unstaking", "1,000.00", "TRX"),
                previewPill("ust-f", PillKind.STAKING, Status.Failed, "Unstaking failed"),
                previewPill("rst-c", PillKind.STAKING, Status.Confirmed, "Rewards restaked", "20.15", "TRX"),
                previewPill("rst-u", PillKind.STAKING, Status.Unconfirmed, "Rewards restaking", "20.15", "TRX"),
                previewPill("rst-f", PillKind.STAKING, Status.Failed, "Rewards restaking failed"),
                previewPill("wd-c", PillKind.STAKING, Status.Confirmed, "Withdraw"),
                previewPill("wd-u", PillKind.STAKING, Status.Unconfirmed, "Withdrawing"),
                previewPill("wd-f", PillKind.STAKING, Status.Failed, "Withdraw failed"),
                previewPill("vt-c", PillKind.STAKING, Status.Confirmed, "Vote"),
                previewPill("vt-u", PillKind.STAKING, Status.Unconfirmed, "Voting"),
                previewPill("vt-f", PillKind.STAKING, Status.Failed, "Vote failed"),
            ),
        )
    }
}

@Suppress("NamedArguments")
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionStatusPill_YieldMode() {
    TangemThemePreviewRedesign {
        PillPreviewColumn(
            items = listOf(
                previewPill("yon-c", PillKind.YIELD_MODE, Status.Confirmed, "Yield mode Enabled"),
                previewPill("yon-u", PillKind.YIELD_MODE, Status.Unconfirmed, "Activating Yield mode"),
                previewPill("yon-f", PillKind.YIELD_MODE, Status.Failed, "Yield mode failed"),
                previewPill("yof-c", PillKind.YIELD_MODE, Status.Confirmed, "Yield mode disabled"),
                previewPill("yof-u", PillKind.YIELD_MODE, Status.Unconfirmed, "Disabling Yield mode"),
                previewPill("yof-f", PillKind.YIELD_MODE, Status.Failed, "Disabling Yield mode failed"),
            ),
        )
    }
}

@Suppress("NamedArguments")
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionStatusPill_Approve() {
    TangemThemePreviewRedesign {
        PillPreviewColumn(
            items = listOf(
                // dApp variant — no subtitle
                previewPill("apv-c", PillKind.APPROVE, Status.Confirmed, "Approved", "2,350.00", "USDT"),
                previewPill("apv-u", PillKind.APPROVE, Status.Unconfirmed, "Approving", "2,350.00", "USDT"),
                previewPill("apv-f", PillKind.APPROVE, Status.Failed, "Approving", "2,350.00", "USDT"),
                // Address variant — with subtitle
                previewPill(
                    txHash = "apa-c",
                    kind = PillKind.APPROVE,
                    status = Status.Confirmed,
                    label = "Approved",
                    amount = "2,350.00",
                    currencySymbol = "USDT",
                    subtitle = PillSubtitle.Address(
                        rawAddress = "33BdfSXXXXXXXXXXXXXXXXXXXXXXga2B",
                        briefAddress = "33BdfS...ga2B",
                    ),
                ),
                previewPill(
                    txHash = "apa-u",
                    kind = PillKind.APPROVE,
                    status = Status.Unconfirmed,
                    label = "Approving",
                    amount = "2,350.00",
                    currencySymbol = "USDT",
                    subtitle = PillSubtitle.Address(
                        rawAddress = "33BdfSXXXXXXXXXXXXXXXXXXXXXXga2B",
                        briefAddress = "33BdfS...ga2B",
                    ),
                ),
                previewPill(
                    txHash = "apa-f",
                    kind = PillKind.APPROVE,
                    status = Status.Failed,
                    label = "Approving",
                    amount = "2,350.00",
                    currencySymbol = "USDT",
                    subtitle = PillSubtitle.Address(
                        rawAddress = "33BdfSXXXXXXXXXXXXXXXXXXXXXXga2B",
                        briefAddress = "33BdfS...ga2B",
                    ),
                ),
            ),
        )
    }
}

// endregion