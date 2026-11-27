package com.tangem.features.polymarket.impl.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun PolymarketOutcomeButtonAnimationPreview() {
    TangemThemePreviewRedesign {
        val prices = listOf("9¢", "10¢", "45.5¢", "46¢", "99.9¢", "100¢", "—", "0.1¢", "45,5¢")
        var index by remember { mutableIntStateOf(0) }
        var price by remember { mutableStateOf(prices.first()) }
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary).statusBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Tap a price to advance", color = TangemTheme.colors3.text.primary)
            for (enabled in listOf(true, false)) {
                CompositionLocalProvider(LocalPolymarketPriceAnimationEnabled provides enabled) {
                    Text(text = "D2: $enabled", color = TangemTheme.colors3.text.primary)
                    PolymarketOutcomeButton(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringReference("A long outcome title that must yield to the price"),
                        price = stringReference(price),
                        appearance = PolymarketOutcomeButton.Appearance.Info,
                        size = PolymarketOutcomeButton.Size.X11,
                        onClick = {
                            index = (index + 1) % prices.size
                            price = prices[index]
                        },
                    )
                }
            }
            PolymarketOutcomeButton(
                title = stringReference("Rapid: 10 → 9 → 8"),
                appearance = PolymarketOutcomeButton.Appearance.Neutral,
                size = PolymarketOutcomeButton.Size.X11,
                onClick = {
                    scope.launch {
                        price = "10¢"
                        delay(400)
                        price = "9¢"
                        delay(80)
                        price = "8¢"
                    }
                },
            )
            PolymarketOutcomeButton(
                title = stringReference("Rapid: 45.5 → 46 → 47"),
                appearance = PolymarketOutcomeButton.Appearance.Neutral,
                size = PolymarketOutcomeButton.Size.X11,
                onClick = {
                    scope.launch {
                        price = "45.5¢"
                        delay(400)
                        price = "46¢"
                        delay(80)
                        price = "47¢"
                    }
                },
            )
        }
    }
}