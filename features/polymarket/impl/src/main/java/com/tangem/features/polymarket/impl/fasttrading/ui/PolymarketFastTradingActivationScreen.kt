package com.tangem.features.polymarket.impl.fasttrading.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.fasttrading.ui.state.PolymarketFastTradingActivationUM

private val ActivationSections = listOf(
    PolymarketFastTradingSection.What,
    PolymarketFastTradingSection.Can,
    PolymarketFastTradingSection.Cant,
    PolymarketFastTradingSection.Control,
)

private val FooterClearance = 120.dp

@Composable
internal fun PolymarketFastTradingActivationScreen(
    state: PolymarketFastTradingActivationUM,
    modifier: Modifier = Modifier,
) {
    ProvideHaze {
        TangemTopBarScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TangemTopNavigation(
                    title = resourceReference(R.string.prediction_session_key_fast_trading),
                    contentAlign = TangemTopNavigation.ContentAlign.Center,
                    onBack = state.onBackClick,
                )
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = contentPadding.calculateTopPadding()),
                ) {
                    Text(
                        text = stringResourceSafe(R.string.prediction_session_key_info_title),
                        style = TangemTheme.typography3.heading.medium,
                        color = TangemTheme.colors3.text.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                    PolymarketFastTradingInfoTiles(onDurationInfoClick = state.onFaqClick)
                    PolymarketFastTradingSections(sections = ActivationSections, firstSeparatedIndex = 1)
                    Spacer(modifier = Modifier.height(FooterClearance))
                }
                TangemFooter(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    TangemButton(
                        modifier = Modifier.fillMaxWidth(),
                        variant = TangemButton.Variant.Primary,
                        size = TangemButton.Size.X12,
                        text = resourceReference(R.string.common_activate),
                        isLoading = state.isActivating,
                        onClick = state.onActivateClick,
                    )
                }
            }
        }
    }
    PolymarketFastTradingFaqSheet(isShown = state.isFaqShown, onDismiss = state.onFaqDismiss)
}

@Preview(name = "Light", widthDp = 390, heightDp = 800)
@Preview(name = "Dark", widthDp = 390, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketFastTradingActivationScreenPreview(
    @PreviewParameter(ActivationPreviewProvider::class) state: PolymarketFastTradingActivationUM,
) {
    TangemThemePreviewRedesign {
        PolymarketFastTradingActivationScreen(state = state)
    }
}

private class ActivationPreviewProvider : PreviewParameterProvider<PolymarketFastTradingActivationUM> {
    override val values = sequenceOf(
        previewState(isActivating = false),
        previewState(isActivating = true),
    )

    private fun previewState(isActivating: Boolean) = PolymarketFastTradingActivationUM(
        isActivating = isActivating,
        isFaqShown = false,
        onBackClick = {},
        onActivateClick = {},
        onFaqClick = {},
        onFaqDismiss = {},
    )
}