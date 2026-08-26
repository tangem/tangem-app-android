package com.tangem.features.polymarket.impl.placeprediction.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingFooter
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.payout
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionAmountBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionMarketBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionNotificationsBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewProvider

@Composable
internal fun PlacePredictionAmountContent(
    state: PlacePredictionUM,
    intents: PlacePredictionIntents,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.prediction_place_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = intents::onBackClick,
                onClose = intents::onCloseClick,
            )
        },
        overlay = { contentPadding ->
            OnboardingFooter(
                contentPadding = contentPadding,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .onSizeChanged { footerHeight = it.height },
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_next),
                    isEnabled = state.isPrimaryButtonEnabled,
                    onClick = intents::onNextClick,
                )
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            TangemSurface(color = TangemTheme.colors3.bg.secondary) {
                PredictionMarketBlock(state = state.market)
            }
            PredictionAmountBlock(
                amountValue = state.amountValue,
                payout = state.quote.payout(),
                payment = state.payment,
                onAmountChange = intents::onAmountChange,
                onAddFundsClick = intents::onAddFundsClick,
            )
            PredictionNotificationsBlock(
                notifications = state.notifications,
                onRetryClick = intents::onQuoteRetryClick,
            )
            SlippageRow(slippage = state.slippage, onClick = intents::onSlippageClick)
            Spacer(modifier = Modifier.height(with(LocalDensity.current) { footerHeight.toDp() }))
        }
    }
}

@Composable
private fun SlippageRow(slippage: SlippageUM, onClick: () -> Unit) {
    TangemSurface(color = TangemTheme.colors3.bg.secondary) {
        TangemRow(
            titleSlot = {
                Text(
                    text = stringResourceSafe(R.string.prediction_place_slippage),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
            },
            valueSlot = {
                Text(
                    text = slippage.label(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            },
            onClick = onClick,
        )
    }
}

@Composable
private fun SlippageUM.label(): String {
    val formatted = "$percent%"

    return if (isDefault) stringResourceSafe(R.string.prediction_place_slippage_default, formatted) else formatted
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePredictionAmountContentPreview(
    @PreviewParameter(PlacePredictionPreviewProvider::class) state: PlacePredictionUM,
) {
    TangemThemePreviewRedesign {
        PlacePredictionAmountContent(state = state, intents = PlacePredictionPreviewIntents)
    }
}