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
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM

@Composable
internal fun TxHistoryDetailsContent(state: TxHistoryDetailsUM, modifier: Modifier = Modifier) {
    when (state) {
        is TxHistoryDetailsUM.SingleAsset -> SingleAssetContent(state = state, modifier = modifier)
        // TODO([REDACTED_TASK_KEY]): two-asset (Swap / Onramp) body — out of scope for the single-asset amount block ticket.
        is TxHistoryDetailsUM.TwoAssets -> TwoAssetsPlaceholder(state = state, modifier = modifier)
    }
}

@Composable
private fun SingleAssetContent(state: TxHistoryDetailsUM.SingleAsset, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        TxHistoryDetailsAmountBlock(amountBlock = state.amountBlock)
        state.counterparty?.let { counterparty ->
            TxHistoryDetailsCounterpartyRow(
                counterparty = counterparty,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
        TxHistoryDetailsInfoRows(
            rows = state.rows,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
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