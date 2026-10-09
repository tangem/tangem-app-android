package com.tangem.features.tangempay.withdraw.bank.preview.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM
import com.tangem.features.tangempay.withdraw.bank.ui.BankFromRow
import com.tangem.features.tangempay.withdraw.bank.ui.BankRouteRow
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TangemPayBankWithdrawPreviewScreen(state: TangemPayBankWithdrawPreviewUM, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayBankWithdrawPreviewUM.Summary -> SummaryScreen(state = state, modifier = modifier)
        is TangemPayBankWithdrawPreviewUM.Failed -> FailedScreen(state = state, modifier = modifier)
    }
}

@Composable
private fun FailedScreen(state: TangemPayBankWithdrawPreviewUM.Failed, modifier: Modifier = Modifier) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Warning,
        title = state.title,
        description = resourceReference(R.string.common_try_again_later),
        asset = {
            TangemStatusIcon(
                kind = state.icon,
                modifier = Modifier.size(StatusIconSize),
                tint = TangemTheme.colors3.icon.status.warning,
            )
        },
        footer = {
            TangemButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FooterPadding),
            ) {
                state.onRetryClick?.let { onRetryClick ->
                    TangemButton(
                        variant = TangemButton.Variant.Secondary,
                        size = TangemButton.Size.X12,
                        text = resourceReference(R.string.common_retry),
                        onClick = onRetryClick,
                    )
                }
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_close),
                    onClick = state.onCloseClick,
                )
            }
        },
    )
}

@Composable
private fun SummaryScreen(state: TangemPayBankWithdrawPreviewUM.Summary, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var footerHeight by remember { mutableStateOf(FooterHeightFallback) }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = state.header.title,
                subtitle = state.header.subtitle,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
                onClose = state.onCloseClick,
            )
        },
        overlay = {
            SendFooter(
                sendingSummary = state.transfer?.sendingSummary,
                isSending = state.isSending,
                isSendEnabled = state.isSendEnabled,
                isColdWallet = state.isColdWallet,
                onSendClick = state.onSendClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = with(density) { size.height.toDp() } },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = footerHeight),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val transfer = state.transfer
            if (transfer != null) {
                AmountCard(amount = transfer.amount)
                BankRouteRow(
                    rail = transfer.route.rail,
                    title = transfer.route.title,
                    eta = transfer.route.eta,
                    fee = transfer.route.fee,
                    onClick = transfer.route.onClick,
                )
            }
            if (state.recipientCards.isNotEmpty()) {
                RecipientInfo(cards = state.recipientCards)
            }
        }
    }
}

@Composable
private fun AmountCard(amount: TangemPayBankWithdrawPreviewUM.AmountCardUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .then(
                amount.onClick?.let { onClick -> Modifier.clickableSingle(role = Role.Button, onClick = onClick) }
                    ?: Modifier,
            ),
    ) {
        BankFromRow(balance = amount.balance, modifier = Modifier.fillMaxWidth())
        TangemRow(
            verticalAlignment = TangemRowVerticalAlignment.Center,
            titleSlot = {
                Text(
                    text = amount.amount,
                    style = TangemTheme.typography3.heading.medium,
                    color = TangemTheme.colors3.text.primary,
                )
            },
            endSlot = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CurrencyIcon(state = amount.icon, iconSize = 40.dp, networkBadgeSize = 12.dp)
                    Text(
                        text = amount.symbol,
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                }
            },
        )
    }
}

@Composable
private fun RecipientInfo(cards: ImmutableList<TangemPayBankWithdrawPreviewUM.RecipientCardUM>) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
        text = stringResourceSafe(R.string.tangempay_withdraw_bank_recipient_info),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
    cards.fastForEach { card -> DetailCard(card = card) }
}

@Composable
private fun DetailCard(card: TangemPayBankWithdrawPreviewUM.RecipientCardUM) {
    val rows = card.rows
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .then(
                card.onClick?.let { onClick -> Modifier.clickableSingle(role = Role.Button, onClick = onClick) }
                    ?: Modifier,
            ),
    ) {
        rows.fastForEachIndexed { index, row ->
            // Figma puts the caption label above the body value, so the row roles are swapped on purpose.
            TangemRow(
                divider = index < rows.lastIndex,
                titleSlot = { TangemRowText(text = row.label, role = TangemRowTextRole.Subtitle) },
                subtitleSlot = { TangemRowText(text = row.value, role = TangemRowTextRole.Title) },
            )
        }
    }
}

@Composable
private fun SendFooter(
    sendingSummary: TextReference?,
    isSending: Boolean,
    isSendEnabled: Boolean,
    isColdWallet: Boolean,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemFooter(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        extraTop = sendingSummary?.let { summary ->
            {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = summary.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_send),
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24).takeIf { isColdWallet },
            isLoading = isSending,
            isEnabled = isSendEnabled,
            onClick = onSendClick,
        )
    }
}

/**
 * Bottom padding used only until [SummaryScreen] measures the real footer (`onSizeChanged`) — avoids a
 * jump on the first frame. Approximates a one-line sending summary: contentPadding (12+12) + button (48)
 * + one caption line (16, [TangemTheme.typography3.caption]) + the gap [TangemFooter] puts between
 * `extraTop` and its content (12). The sending-summary sentence is localized into several languages and
 * interpolates formatted amounts, so it can wrap to two lines under some locale/font-scale combinations —
 * the measured height, not this constant, is what actually clears the footer once available.
 */
private val FooterHeightFallback = 100.dp
private val StatusIconSize = 40.dp
private val FooterPadding = 16.dp

private class PreviewProvider : PreviewParameterProvider<TangemPayBankWithdrawPreviewUM> {
    override val values: Sequence<TangemPayBankWithdrawPreviewUM> = sequenceOf(
        TangemPayBankWithdrawPreviewData.summary,
        TangemPayBankWithdrawPreviewData.summaryBusiness,
        TangemPayBankWithdrawPreviewData.summarySavedRecipient,
        TangemPayBankWithdrawPreviewData.summaryHotWallet,
        TangemPayBankWithdrawPreviewData.sending,
        TangemPayBankWithdrawPreviewData.failedGeneral,
        TangemPayBankWithdrawPreviewData.failedUnavailable,
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TangemPayBankWithdrawPreviewScreen(
    @PreviewParameter(PreviewProvider::class) state: TangemPayBankWithdrawPreviewUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawPreviewScreen(state = state)
    }
}