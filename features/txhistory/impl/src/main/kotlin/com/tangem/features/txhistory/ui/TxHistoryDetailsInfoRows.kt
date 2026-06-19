package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.InfoRowUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Info-rows block of the transaction details card: a vertical list of DS3 [TangemRow]s (label on the leading side,
 * value on the trailing side — e.g. `Network fee`, `Rate`).
 *
 * Divider handling matches the design: a single row renders without a divider, while a multi-row block draws an inset
 * bottom divider under every row except the last. The same block therefore serves both the single-asset card (one
 * `Network fee` row) and the two-asset / exchange card (`Network fee` + `Rate` + …).
 *
 * The value is rendered in `text/secondary` to match the design — [TangemRowText]'s `Value` role is primary-colored, so
 * the trailing slot uses a plain [Text] tuned to body/medium + secondary instead.
 *
 * @param rows Rows to render in order. An empty list renders nothing — callers should skip the block when empty.
 * @param modifier Modifier applied to the list container.
 */
@Composable
internal fun TxHistoryDetailsInfoRows(rows: ImmutableList<InfoRowUM>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return
    Column(
        modifier = modifier,
    ) {
        val lastIndex = rows.lastIndex
        rows.forEachIndexed { index, row ->
            TangemRow(
                divider = index < lastIndex,
                contentLead = TangemRowContentLead.Start,
                titleSlot = { TangemRowText(text = row.label, role = TangemRowTextRole.Title) },
                valueSlot = {
                    Text(
                        text = row.value.resolveReference(),
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.body.medium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

// region Preview

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TxHistoryDetailsInfoRowsPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary).padding(16.dp),
        ) {
            // Multiple rows — dividers between rows, none after the last
            TxHistoryDetailsInfoRows(
                rows = persistentListOf(
                    InfoRowUM(label = stringReference("Network fee"), value = stringReference("0.00056 ETH")),
                    InfoRowUM(label = stringReference("Rate"), value = stringReference("1 POL ≈ 0.36 USDT")),
                    InfoRowUM(label = stringReference("Network fee"), value = stringReference("0.00056 ETH")),
                ),
            )
            // Single row — no divider
            TxHistoryDetailsInfoRows(
                modifier = Modifier.padding(top = 16.dp),
                rows = persistentListOf(
                    InfoRowUM(label = stringReference("Network fee"), value = stringReference("0.00056 ETH")),
                ),
            )
        }
    }
}

// endregion