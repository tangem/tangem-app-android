package com.tangem.features.txhistory.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.cosign.CosignRequestRow
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM

@Composable
internal fun TxHistoryDetailsContent(
    state: TxHistoryDetailsUM,
    modifier: Modifier = Modifier,
    ratingContent: (@Composable (Modifier) -> Unit)? = null,
) {
    when (state) {
        is TxHistoryDetailsUM.SingleAsset -> SingleAssetContent(state = state, modifier = modifier)
        is TxHistoryDetailsUM.TwoAssets -> TwoAssetsContent(
            state = state,
            modifier = modifier,
            ratingContent = ratingContent,
        )
    }
}

@Composable
private fun SingleAssetContent(state: TxHistoryDetailsUM.SingleAsset, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        TxHistoryDetailsAmountBlock(amountBlock = state.amountBlock, isBalanceHidden = state.isBalanceHidden)
        state.counterparty?.let { counterparty ->
            TxHistoryDetailsCounterpartyRow(
                counterparty = counterparty,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
        state.cosign?.let { cosign ->
            CosignRequestRow(
                state = cosign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
        TxHistoryDetailsStatusBanner(
            state = state.statusBanner,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        TxHistoryDetailsInfoRows(
            rows = state.rows,
            isBalanceHidden = state.isBalanceHidden,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (state.statusBanner != null) 16.dp else 0.dp,
                    bottom = 16.dp,
                ),
        )
        state.cosignAction?.let { cosignAction ->
            CosignActionBlock(
                state = cosignAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

/** Sign/Signed/Rejected CTA + its caption, mirroring the [TxHistoryDetailsUM.ProviderButtonUM] block layout. */
@Composable
private fun CosignActionBlock(state: TxHistoryDetailsUM.CosignActionUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            text = state.buttonText,
            isEnabled = state.isEnabled,
            isLoading = state.isLoading,
            iconEnd = state.iconEnd,
            onClick = state.onClick,
            size = TangemButton.Size.X12,
        )
        Text(
            text = state.caption.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun TwoAssetsContent(
    state: TxHistoryDetailsUM.TwoAssets,
    modifier: Modifier = Modifier,
    ratingContent: (@Composable (Modifier) -> Unit)? = null,
) {
    val from = state.from
    val to = state.to
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (from != null && to != null) {
            TxHistoryDetailsTwoAssetsBlock(
                from = from,
                to = to,
                isBalanceHidden = state.isBalanceHidden,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
            )
        } else {
            // Safety fallback for a future express variant that yields no asset legs — render the header-only card.
            TwoAssetsPlaceholder(state = state)
        }
        // Express status plaque under the exchange block. The top gap is owned by the banner (inside its collapsing
        // region), so only horizontal padding is applied here.
        TxHistoryDetailsStatusBanner(
            state = state.statusBanner,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        // Provider-rating (CSAT) card — final express swaps only. The card owns the spacing it is given, so an
        // already-rated deal, which draws nothing, costs no gap.
        ratingContent?.invoke(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        // Network fee (and later rate) pulled from the matched on-chain leg; the block is skipped when [rows] is empty.
        TxHistoryDetailsInfoRows(
            rows = state.rows,
            isBalanceHidden = state.isBalanceHidden,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        // Bottom "Go to provider" / "Go to verification" CTA — only on a provider-actionable terminal with a link.
        state.providerButton?.let { providerButton ->
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                variant = TangemButton.Variant.Primary,
                text = providerButton.text,
                iconEnd = TangemIconUM.Icon(Icons.ic_chevron_right_20),
                onClick = providerButton.onClick,
            )
        }
    }
}

@Composable
private fun TwoAssetsPlaceholder(state: TxHistoryDetailsUM.TwoAssets, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = state.header.title.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.medium,
            textAlign = TextAlign.Center,
        )
    }
}