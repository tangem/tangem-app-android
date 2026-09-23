package com.tangem.features.polymarket.impl.placeprediction.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PredictionFeeSheetUMConverter
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewProvider

@Composable
internal fun PredictionFeeSheet(state: PredictionFeeSheetUM, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        title = {
            TangemModal.TopNavigation(title = resourceReference(R.string.prediction_place_fee))
        },
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            state.rows.forEachIndexed { index, row ->
                FeeRow(row = row, divider = index < state.rows.lastIndex)
            }
        }
    }
}

@Composable
private fun FeeRow(row: PredictionFeeSheetUM.Row, divider: Boolean) {
    TangemRow(
        divider = divider,
        contentLead = TangemRowContentLead.Start,
        titleSlot = {
            TangemRowText(text = row.title, role = TangemRowTextRole.Title, maxLines = Int.MAX_VALUE)
        },
        valueSlot = {
            Text(
                text = row.amount,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.End,
            )
        },
        extraBottomSlot = row.description?.let { description ->
            {
                TangemRowText(
                    text = description,
                    role = TangemRowTextRole.Subtitle,
                    maxLines = Int.MAX_VALUE,
                )
            }
        },
    )
}

@Preview(name = "Light", widthDp = 360, heightDp = 800)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PredictionFeeSheetPreview() {
    val state = PlacePredictionPreviewProvider().values.firstNotNullOf { preview ->
        (preview.quote as? QuoteUM.Content)?.let { content ->
            PredictionFeeSheetUMConverter.convert(
                content.signable.quote,
                preview.slippage,
                preview.collateral.tokenSymbol,
            )
        }
    }
    TangemThemePreviewRedesign {
        PredictionFeeSheet(state = state, onDismiss = {})
    }
}