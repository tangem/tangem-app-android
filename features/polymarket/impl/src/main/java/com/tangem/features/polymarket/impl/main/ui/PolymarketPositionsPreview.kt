package com.tangem.features.polymarket.impl.main.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadingState
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.positions.ui.PolymarketPositionCard

@Composable
internal fun PolymarketPositionsPreview(state: PolymarketPositionsPreviewUM, modifier: Modifier = Modifier) {
    if (!state.isVisible) return

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = "My Predictions",
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.small,
        )
        when (state) {
            PolymarketPositionsPreviewUM.Loading -> PolymarketLoadingState()
            is PolymarketPositionsPreviewUM.Error -> PolymarketReloadPrompt(
                text = stringReference("Couldn't load positions. Tap to reload"),
                onReloadClick = state.onRetry,
            )
            is PolymarketPositionsPreviewUM.Content -> {
                state.positions.forEach { PolymarketPositionCard(state = it) }
                state.onRetry?.let { onRetry ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = onRetry)
                            .heightIn(min = 48.dp)
                            .padding(vertical = 12.dp),
                        text = "Data may be outdated. Tap to reload",
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.caption.medium,
                    )
                }
            }
        }
    }
}