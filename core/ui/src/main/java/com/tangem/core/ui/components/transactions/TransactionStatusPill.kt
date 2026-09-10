package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.test.TransactionHistoryItemTestTags
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
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_16

private const val IDENTICON_INLINE_ID = "pill_identicon"
private val IDENTICON_SIZE = 16.dp

@Composable
internal fun TransactionStatusPill(
    state: TransactionItemUM.Pill,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TransactionHistoryItemTestTags.ITEM)
            .clickable(onClick = state.onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Pill(state = state, isBalanceHidden = isBalanceHidden)
    }
}

@Composable
private fun Pill(state: TransactionItemUM.Pill, isBalanceHidden: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(state.status.backgroundColor())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LeadingIcon(kind = state.kind, status = state.status)
        PillText(state = state, isBalanceHidden = isBalanceHidden)
    }
}

/**
 * The pill body as a single line: the action label in the label color, the value pieces — amount, currency symbol,
 * counterparty address — in the value color, and the address' identicon as inline content.
 *
 * One [Text] rather than a row of them, so the capsule ellipsizes as a whole instead of letting a long address push
 * it past the screen edge, and the gaps are plain spaces as the design has them. The amount is masked before it
 * enters the string, so hiding balances stars out the number alone.
 */
@Composable
private fun PillText(state: TransactionItemUM.Pill, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    val labelStyle = SpanStyle(color = state.status.labelColor())
    val valueStyle = SpanStyle(color = state.status.valueColor())
    val isFailed = state.status is Status.Failed
    val label = state.label.resolveReference()
    val amount = state.amount?.orMaskWithStars(isBalanceHidden)?.resolveReference()
    // A failed pill with an amount reads "<action> <amount> <symbol> failed" — the template wraps the whole body,
    // which the failed state paints in one color anyway, so it needs no span split.
    val failedBody = if (isFailed && amount != null) {
        stringResourceSafe(R.string.common_action_failed, state.failedBody(isBalanceHidden))
    } else {
        null
    }
    val address = (state.subtitle as? PillSubtitle.Address)?.takeIf { !isFailed }
    val addressTemplate = address?.let {
        stringResourceSafe(R.string.transaction_history_to_inline_address, it.briefAddress)
    }
    val identiconSizeSp = with(LocalDensity.current) { IDENTICON_SIZE.toSp() }
    val placeholder = remember(identiconSizeSp) {
        Placeholder(
            width = identiconSizeSp,
            height = identiconSizeSp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        )
    }
    val text = remember(label, amount, state.currencySymbol, failedBody, addressTemplate, labelStyle, valueStyle) {
        buildAnnotatedString {
            if (failedBody != null) {
                withStyle(labelStyle) { append(failedBody) }
            } else {
                withStyle(labelStyle) { append(label) }
                amount?.let { withStyle(valueStyle) { append(" $it") } }
                state.currencySymbol?.let { withStyle(valueStyle) { append(" $it") } }
                addressTemplate?.let { template ->
                    // The template keeps its own spacing around the marker, e.g. "to: %image% 33Bd...ga2B"
                    withStyle(labelStyle) { append(" " + template.substringBefore(INLINE_IMAGE_PLACEHOLDER)) }
                    appendInlineContent(IDENTICON_INLINE_ID, INLINE_IMAGE_PLACEHOLDER)
                    withStyle(valueStyle) {
                        append(template.substringAfter(INLINE_IMAGE_PLACEHOLDER, missingDelimiterValue = ""))
                    }
                }
            }
        }
    }
    Text(
        text = text,
        inlineContent = address?.let { subtitle ->
            mapOf(
                IDENTICON_INLINE_ID to InlineTextContent(placeholder = placeholder) {
                    IdentIcon(
                        address = subtitle.rawAddress,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                },
            )
        }.orEmpty(),
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun LeadingIcon(kind: PillKind, status: Status) {
    if (status is Status.Unconfirmed) {
        CircularProgressIndicator(
            strokeWidth = 1.5.dp,
            color = TangemTheme.colors3.icon.status.info,
            modifier = Modifier.size(16.dp),
        )
        return
    }
    val icon = when (status) {
        is Status.Failed -> Icons.ic_cross_16
        is Status.Confirmed -> when (kind) {
            PillKind.STAKING -> ImageVector.vectorResource(R.drawable.ic_transaction_history_staking_24)
            PillKind.YIELD_MODE -> ImageVector.vectorResource(R.drawable.ic_yield_mode_16)
            PillKind.APPROVE -> null
        }
        is Status.Unconfirmed -> null
    } ?: return
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = status.iconTint(),
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun Status.backgroundColor(): Color = when (this) {
    is Status.Confirmed -> TangemTheme.colors3.bg.opaque.secondary
    is Status.Unconfirmed -> TangemTheme.colors3.bg.status.infoSubtle
    is Status.Failed -> TangemTheme.colors3.bg.status.errorSubtle
}

@Composable
private fun Status.labelColor(): Color = when (this) {
    is Status.Confirmed -> TangemTheme.colors3.text.secondary
    is Status.Unconfirmed -> TangemTheme.colors3.text.status.info
    is Status.Failed -> TangemTheme.colors3.text.status.error
}

@Composable
private fun Status.valueColor(): Color = when (this) {
    is Status.Confirmed -> TangemTheme.colors3.text.primary
    is Status.Unconfirmed -> TangemTheme.colors3.text.status.info
    is Status.Failed -> TangemTheme.colors3.text.status.error
}

/** The leading icon always carries the label color of its state, so the pill reads as one piece. */
@Composable
private fun Status.iconTint(): Color = when (this) {
    is Status.Confirmed -> TangemTheme.colors3.icon.secondary
    is Status.Unconfirmed -> TangemTheme.colors3.icon.status.info
    is Status.Failed -> TangemTheme.colors3.icon.status.error
}

@Composable
private fun TransactionItemUM.Pill.failedBody(isBalanceHidden: Boolean): String = buildString {
    append(label.resolveReference())
    amount?.let { value ->
        append(' ')
        append(value.orMaskWithStars(isBalanceHidden).resolveReference())
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
    amount = amount?.let { stringReference(it) },
    currencySymbol = currencySymbol,
    subtitle = subtitle,
    timestamp = 0L,
    onClick = {},
)

@Composable
private fun PillPreviewColumn(items: List<TransactionItemUM.Pill>) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors3.bg.primary)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                // Unlimited allowance variant — "Unlimited" stands in for the number
                previewPill(
                    txHash = "apa-unlimited",
                    kind = PillKind.APPROVE,
                    status = Status.Confirmed,
                    label = "Approved",
                    amount = "Unlimited",
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