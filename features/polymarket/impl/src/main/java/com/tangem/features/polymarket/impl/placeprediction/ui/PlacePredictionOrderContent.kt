package com.tangem.features.polymarket.impl.placeprediction.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountPresetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionOrderIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionAmountPresets
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionNotificationsBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionOrderDetailsCard
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionOrderHeader
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionPlaceButton
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionPlaceFooter
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionOrderPreviewProvider
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PreviewAmountPresets
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PlacePredictionOrderContent(
    state: PlacePredictionUM,
    presets: ImmutableList<AmountPresetUM>,
    intents: PlacePredictionOrderIntents,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }
    val isIdle = state.submit is SubmitUM.Idle

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                endButton = { TangemButton.Close(onClick = intents::onCloseClick) },
                contentColumn = { PredictionOrderHeader(market = state.market) },
            )
        },
        overlay = { _ ->
            PredictionPlaceFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .onSizeChanged { footerHeight = it.height },
                onPolymarketTermsClick = intents::onPolymarketTermsClick,
                onTangemTermsClick = intents::onTangemTermsClick,
            ) {
                PredictionPlaceButton(
                    order = state.order,
                    isLoading = !isIdle,
                    isEnabled = state.isPrimaryButtonEnabled,
                    onClick = intents::onPlaceClick,
                )
            }
        },
    ) { contentPadding ->
        OrderBody(
            state = state,
            presets = presets,
            intents = intents,
            contentPadding = contentPadding,
            footerHeight = footerHeight,
        )
    }
}

@Composable
private fun OrderBody(
    state: PlacePredictionUM,
    presets: ImmutableList<AmountPresetUM>,
    intents: PlacePredictionOrderIntents,
    contentPadding: PaddingValues,
    footerHeight: Int,
) {
    Column(
        modifier = Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            PredictionOrderAmountInput(
                state = state,
                onAmountChange = intents::onAmountChange,
                onAddFundsClick = intents::onAddFundsClick,
            )
        }
        if (presets.isNotEmpty()) {
            PredictionAmountPresets(
                presets = presets,
                isEnabled = state.submit is SubmitUM.Idle,
                onPresetClick = intents::onPresetClick,
                onMaxClick = intents::onMaxClick,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            PredictionOrderDetailsCard(
                slippage = state.slippage,
                total = (state.quote as? QuoteUM.Content)?.total?.formatPolymarketMoney().orEmpty(),
                isTotalLoading = state.quote is QuoteUM.Loading,
                onSlippageClick = intents::onSlippageClick,
                onTotalClick = intents::onFeeClick.takeIf { state.canShowFeeBreakdown() },
            )
            PredictionNotificationsBlock(
                notifications = state.notifications,
                onRetryClick = intents::onQuoteRetryClick,
            )
        }
        Spacer(modifier = Modifier.height(with(LocalDensity.current) { footerHeight.toDp() }))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePredictionOrderContentPreview(
    @PreviewParameter(PlacePredictionOrderPreviewProvider::class) state: PlacePredictionUM,
) {
    TangemThemePreviewRedesign {
        PlacePredictionOrderContent(
            state = state,
            presets = PreviewAmountPresets,
            intents = PlacePredictionPreviewIntents,
        )
    }
}