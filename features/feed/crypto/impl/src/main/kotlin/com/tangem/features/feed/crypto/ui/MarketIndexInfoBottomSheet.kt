package com.tangem.features.feed.crypto.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.formatAnnotatedWithUrls
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.ui.state.MarketIndexStatus
import com.tangem.features.feed.crypto.ui.state.MarketIndexUM
import com.tangem.utils.StringsSigns

internal data class MarketIndexInfoBottomSheetContent(
    val index: MarketIndexUM,
) : TangemBottomSheetConfigContent

/**
 * Info sheet of a market index: the reading with its band label and scale, plus what the index means.
 *
 * A [TangemModal] rather than a `TangemBottomSheet` because the crypto tab lives inside the feed
 * shtorka — the modal registers with the host installed at the app root, so it floats above the
 * shtorka instead of opening a competing dialog window.
 */
@Composable
internal fun MarketIndexInfoBottomSheet(
    state: MarketIndexInfoBottomSheetContent,
    onLinkClick: (url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    TangemModal<MarketIndexInfoBottomSheetContent>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = state,
        ),
        title = {
            TangemModal.TopNavigation(
                endButton = { TangemButton.Close(onClick = onDismiss) },
            )
        },
        content = { content -> Content(index = content.index, onLinkClick = onLinkClick) },
    )
}

@Composable
private fun Content(index: MarketIndexUM, onLinkClick: (url: String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 40.dp),
    ) {
        Text(
            text = combinedReference(
                index.fullTitle,
                stringReference("${StringsSigns.WHITE_SPACE}${StringsSigns.MINUS}${StringsSigns.WHITE_SPACE}"),
                index.value,
            ).resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.border.tertiary,
        )
        SpacerH(4.dp)
        Text(
            text = index.statusLabel.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = index.status.textColor(),
        )
        IndexScaleIndicator(
            progress = index.progress,
            size = IndexScaleIndicatorSize.Big,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 12.dp),
        )
        Text(
            text = formatAnnotatedWithUrls(
                text = index.description.resolveReference(),
                linkColor = TangemTheme.colors3.text.accent.blue,
                onLinkClick = onLinkClick,
            ),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MarketIndexInfoBottomSheetFearGreedPreview() {
    TangemThemePreviewRedesign {
        MarketIndexInfoBottomSheet(state = previewConfig(index = previewFearGreed), onLinkClick = {}, onDismiss = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MarketIndexInfoBottomSheetAltcoinIndexPreview() {
    TangemThemePreviewRedesign {
        MarketIndexInfoBottomSheet(state = previewConfig(index = previewAltcoinIndex), onLinkClick = {}, onDismiss = {})
    }
}

private fun previewConfig(index: MarketIndexUM) = MarketIndexInfoBottomSheetContent(
    index = index,
)

private val previewFearGreed = MarketIndexUM(
    id = "fear_greed",
    title = resourceReference(R.string.market_crypto_total_cap_fear_greed_title_short),
    fullTitle = resourceReference(R.string.market_crypto_total_cap_fear_greed_title_full),
    description = resourceReference(R.string.market_crypto_total_cap_fear_greed_info),
    statusLabel = stringReference("Greed"),
    status = MarketIndexStatus.Info,
    value = stringReference("73"),
    progress = 0.73f,
)

private val previewAltcoinIndex = MarketIndexUM(
    id = "altcoin_index",
    title = resourceReference(R.string.market_crypto_total_cap_altcoin_index_title),
    fullTitle = resourceReference(R.string.market_crypto_total_cap_altcoin_index_title),
    description = resourceReference(R.string.market_crypto_total_cap_altcoin_index_info),
    statusLabel = stringReference("Balanced Market"),
    status = MarketIndexStatus.Warning,
    value = stringReference("45"),
    progress = 0.45f,
)
// endregion