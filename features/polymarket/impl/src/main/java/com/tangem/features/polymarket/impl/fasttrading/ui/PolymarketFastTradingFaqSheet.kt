package com.tangem.features.polymarket.impl.fasttrading.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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

private val FaqSections = PolymarketFastTradingSection.entries

@Composable
internal fun PolymarketFastTradingFaqSheet(isShown: Boolean, onDismiss: () -> Unit) {
    TangemModal<FaqSheetContent>(
        config = TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = onDismiss,
            content = FaqSheetContent,
        ),
        title = {
            TangemModal.TopNavigation(
                title = resourceReference(R.string.prediction_session_key_fast_trading),
                onClose = onDismiss,
            )
        },
        content = {
            FaqContent()
        },
    )
}

@Composable
private fun FaqContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(bottom = 24.dp)) {
        Text(
            text = stringResourceSafe(R.string.prediction_session_key_info_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.padding(24.dp),
        )
        PolymarketFastTradingInfoTiles(onDurationInfoClick = null)
        PolymarketFastTradingSections(sections = FaqSections, firstSeparatedIndex = 2)
    }
}

@Immutable
private data object FaqSheetContent : TangemBottomSheetConfigContent

@Preview(name = "Light", widthDp = 390)
@Preview(name = "Dark", widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketFastTradingFaqSheetPreview() {
    TangemThemePreviewRedesign {
        PolymarketFastTradingFaqSheet(isShown = true, onDismiss = {})
    }
}