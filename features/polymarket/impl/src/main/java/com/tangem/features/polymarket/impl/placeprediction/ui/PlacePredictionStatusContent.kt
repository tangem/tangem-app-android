package com.tangem.features.polymarket.impl.placeprediction.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingFooter
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionMarketBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlaceResultPreviewProvider

/**
 * Terminal screen of the flow, in three variants that differ only in their glow, title and banner.
 *
 * A submission that never reached the book is deliberately not one of them: that is a dialog over the
 * summary, because the same order can still be placed.
 */
@Composable
internal fun PlacePredictionStatusContent(
    result: PlaceResultUM,
    market: MarketHeaderUM,
    intents: PlacePredictionIntents,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = { TangemTopNavigation(endButton = { TangemButton.Close(onClick = intents::onCloseClick) }) },
        overlay = { contentPadding ->
            OnboardingFooter(
                contentPadding = contentPadding,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { footerHeight = it.height },
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.prediction_place_result_to_main),
                    onClick = intents::onCloseClick,
                )
            }
        },
    ) { contentPadding ->
        TangemGlowRing(modifier = Modifier.fillMaxSize(), variant = result.glowVariant(), cornerRadius = 0.dp)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            ResultHeadline(result = result)
            ResultBanner(result = result)
            TangemSurface(color = TangemTheme.colors3.bg.secondary) {
                PredictionMarketBlock(state = market)
            }
            Spacer(modifier = Modifier.height(with(LocalDensity.current) { footerHeight.toDp() }))
        }
    }
}

@Composable
private fun ResultHeadline(result: PlaceResultUM) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(result.titleRes()),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        if (result is PlaceResultUM.PartiallyFilled) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResourceSafe(
                    R.string.prediction_place_result_partial_amount,
                    result.filledAmount.formatPolymarketMoney(),
                    result.requestedAmount.formatPolymarketMoney(),
                ),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ResultBanner(result: PlaceResultUM) {
    when (result) {
        is PlaceResultUM.Filled -> Unit
        is PlaceResultUM.PartiallyFilled -> TangemMessageBanner(
            title = resourceReference(R.string.prediction_place_result_partial_banner_title),
            description = resourceReference(R.string.prediction_place_result_partial_banner_description),
            variant = TangemMessageBanner.Variant.Warning,
        )
        is PlaceResultUM.NotFilled -> TangemMessageBanner(
            title = resourceReference(R.string.prediction_place_result_no_fill_banner_title),
            description = resourceReference(R.string.prediction_place_result_no_fill_banner_description),
            variant = TangemMessageBanner.Variant.Error,
        )
    }
}

private fun PlaceResultUM.titleRes(): Int = when (this) {
    is PlaceResultUM.Filled -> R.string.prediction_place_result_success_title
    is PlaceResultUM.PartiallyFilled -> R.string.prediction_place_result_partial_title
    is PlaceResultUM.NotFilled -> R.string.prediction_place_result_no_fill_title
}

private fun PlaceResultUM.glowVariant(): TangemGlowRing.Variant = when (this) {
    is PlaceResultUM.Filled -> TangemGlowRing.Variant.Success
    is PlaceResultUM.PartiallyFilled -> TangemGlowRing.Variant.Warning
    is PlaceResultUM.NotFilled -> TangemGlowRing.Variant.Error
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePredictionStatusContentPreview(
    @PreviewParameter(PlaceResultPreviewProvider::class) result: PlaceResultUM,
) {
    TangemThemePreviewRedesign {
        PlacePredictionStatusContent(
            result = result,
            market = MarketHeaderUM(
                title = "Will Uzbekistan win the 2026 FIFA World Cup?",
                imageUrl = null,
                outcomeTitle = "No",
                outcomePriceCents = 85,
            ),
            intents = PlacePredictionPreviewIntents,
        )
    }
}