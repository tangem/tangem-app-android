package com.tangem.features.polymarket.impl.positiondetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Pnl

@Composable
internal fun PolymarketPositionPnlSheet(pnl: Pnl?, onDismiss: () -> Unit) {
    val displayedPnl = rememberLastNonNull(pnl) ?: return
    val (title, description) = when (displayedPnl) {
        Pnl.Unrealized -> R.string.prediction_positions_unrealized_pnl to
            R.string.prediction_positions_unrealized_description
        Pnl.Realized -> R.string.prediction_positions_realized_pnl to R.string.prediction_positions_realized_description
    }
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = pnl != null,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = {
            TangemModal.TopNavigation(title = resourceReference(title))
        },
    ) {
        Text(
            text = stringResourceSafe(description),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
        )
    }
}

private class PnlPreviewProvider : CollectionPreviewParameterProvider<Pnl>(Pnl.entries)

@Preview(name = "Light", widthDp = 360, heightDp = 800)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PolymarketPositionPnlSheetPreview(@PreviewParameter(PnlPreviewProvider::class) pnl: Pnl) {
    TangemThemePreviewRedesign { PolymarketPositionPnlSheet(pnl = pnl, onDismiss = {}) }
}