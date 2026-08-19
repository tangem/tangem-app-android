package com.tangem.features.polymarket.impl.placeprediction.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionModel

internal class PlacePredictionAmountComponent(
    appComponentContext: AppComponentContext,
    private val model: PlacePredictionModel,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        Column(
            modifier = modifier.background(TangemTheme.colors3.bg.tertiary),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Text(text = "${state.market.title} — ${state.market.outcomeTitle} ${state.market.outcomePriceCents}¢")
            Text(text = "Balance: ${state.payment.balance} ${state.payment.tokenSymbol}")
            TextField(value = state.amountValue, onValueChange = model::onAmountChange)
            Text(text = "Quote: ${state.quote}")
            Text(text = "Notifications: ${state.notifications}")
            Button(onClick = model::onNextClick, enabled = state.isPrimaryButtonEnabled) {
                Text(text = "Next")
            }
        }
    }
}