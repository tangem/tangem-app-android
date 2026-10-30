package com.tangem.features.polymarket.impl.placeprediction.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.payout
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSummaryIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionDetailsBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionMarketBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionNotificationsBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionPaymentBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionPlaceButton
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionPlaceFooter
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewProvider

@Composable
internal fun PlacePredictionSummaryContent(
    state: PlacePredictionUM,
    intents: PlacePredictionSummaryIntents,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(
                    when (state.order) {
                        is OrderSideUM.Buy -> R.string.prediction_place_summary_title
                        is OrderSideUM.Sell -> R.string.prediction_close_title
                    },
                ),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = intents::onBackClick,
                onClose = intents::onCloseClick,
            )
        },
        overlay = { _ ->
            PredictionPlaceFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { footerHeight = it.height },
                onPolymarketTermsClick = intents::onPolymarketTermsClick,
                onTangemTermsClick = intents::onTangemTermsClick,
            ) {
                PredictionPlaceButton(
                    order = state.order,
                    isLoading = state.submit !is SubmitUM.Idle,
                    isEnabled = state.isPrimaryButtonEnabled,
                    onClick = intents::onPlaceClick,
                )
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            PredictionPaymentBlock(
                amountValue = state.amountValue,
                payout = state.quote.payout(),
                order = state.order,
            )
            PredictionMarketBlock(state = state.market)
            PredictionDetailsBlock(state = state, onFeeClick = intents::onFeeClick)
            PredictionNotificationsBlock(
                notifications = state.notifications,
                onRetryClick = intents::onQuoteRetryClick,
            )
            Spacer(modifier = Modifier.height(with(LocalDensity.current) { footerHeight.toDp() }))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePredictionSummaryContentPreview(
    @PreviewParameter(PlacePredictionPreviewProvider::class) state: PlacePredictionUM,
) {
    TangemThemePreviewRedesign {
        PlacePredictionSummaryContent(state = state, intents = PlacePredictionPreviewIntents)
    }
}