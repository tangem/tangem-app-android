package com.tangem.features.polymarket.impl.positiondetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun PolymarketMarketClosedSheet(isShown: Boolean, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = {
            TangemModal.TopNavigation(
                title = resourceReference(R.string.prediction_place_notification_market_closed_title),
            )
        },
    ) {
        Text(
            text = stringResourceSafe(R.string.prediction_place_notification_market_closed_description),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
        )
    }
}

@Preview(name = "Light", widthDp = 360, heightDp = 800)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PolymarketMarketClosedSheetPreview() {
    TangemThemePreviewRedesign { PolymarketMarketClosedSheet(isShown = true, onDismiss = {}) }
}