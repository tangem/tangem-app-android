package com.tangem.features.polymarket.impl.positiondetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.positiondetails.ui.preview.PolymarketFinishedPositionPreviewProvider
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketFinishedPositionUM

@Composable
internal fun PolymarketFinishedPositionSheet(state: PolymarketFinishedPositionUM?, onDismiss: () -> Unit) {
    val displayed = rememberLastNonNull(state) ?: return
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = state != null,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.prediction_positions_finished_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                fadeEnabled = false,
                blurBackground = false,
            )
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 32.dp, bottom = 24.dp)) {
            PolymarketPositionSummary(state = displayed.summary, modifier = Modifier.padding(bottom = 32.dp))
            if (displayed.shouldShowOutcome) {
                TangemRow(
                    divider = displayed.resolutionDate != null || displayed.closureDate != null,
                    titleSlot = {
                        TangemRowText(resourceReference(R.string.prediction_positions_outcome), TangemRowTextRole.Title)
                    },
                    valueSlot = {
                        TangemMessageBubble(
                            text = displayed.summary.outcomeLabel,
                            variant = displayed.summary.outcomeVariant,
                        )
                    },
                )
            }
            displayed.resolutionDate?.let { date ->
                FinishedDateRow(
                    title = resourceReference(R.string.prediction_details_resolution_date),
                    value = date,
                    divider = displayed.closureDate != null,
                )
            }
            displayed.closureDate?.let { date ->
                FinishedDateRow(title = resourceReference(R.string.prediction_positions_closure_date), value = date)
            }
        }
    }
}

@Composable
private fun FinishedDateRow(title: TextReference, value: TextReference, divider: Boolean = false) {
    TangemRow(
        divider = divider,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title, maxLines = Int.MAX_VALUE) },
        valueSlot = { TangemRowText(text = value, role = TangemRowTextRole.Value, maxLines = Int.MAX_VALUE) },
    )
}

@Preview(name = "Light", widthDp = 360, heightDp = 800)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PolymarketFinishedPositionSheetPreview(
    @PreviewParameter(PolymarketFinishedPositionPreviewProvider::class) state: PolymarketFinishedPositionUM,
) {
    TangemThemePreviewRedesign { PolymarketFinishedPositionSheet(state = state, onDismiss = {}) }
}