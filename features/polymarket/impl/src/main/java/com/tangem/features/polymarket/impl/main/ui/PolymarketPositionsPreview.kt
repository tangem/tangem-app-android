package com.tangem.features.polymarket.impl.main.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_16
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadingState
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.positions.ui.PolymarketPositionCard

@Composable
internal fun PolymarketPositionsPreview(
    state: PolymarketPositionsPreviewUM,
    modifier: Modifier = Modifier,
    onActiveClick: (() -> Unit)? = null,
    onFinishedClick: (() -> Unit)? = null,
) {
    if (!state.isVisible && onFinishedClick == null) return

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResourceSafe(R.string.prediction_positions_main_title),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.heading.small,
            )
            onFinishedClick?.let { onClick ->
                TangemButton(
                    text = resourceReference(R.string.prediction_positions_finished),
                    iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_chevron_right_16),
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X9,
                    onClick = onClick,
                )
            }
        }
        when (state) {
            PolymarketPositionsPreviewUM.Loading -> PolymarketLoadingState()
            is PolymarketPositionsPreviewUM.Error -> PolymarketReloadPrompt(
                text = resourceReference(R.string.prediction_positions_load_error),
                onReloadClick = state.onRetry,
            )
            is PolymarketPositionsPreviewUM.Content -> {
                state.positions.forEach { PolymarketPositionCard(state = it) }
                if (state.positions.isEmpty() && state.onRetry == null) {
                    Text(
                        text = stringResourceSafe(R.string.prediction_positions_empty_active),
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.body.medium,
                    )
                }
                state.onRetry?.let { onRetry ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = onRetry)
                            .heightIn(min = 48.dp)
                            .padding(vertical = 12.dp),
                        text = stringResourceSafe(R.string.prediction_main_data_outdated),
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.caption.medium,
                    )
                }
            }
        }
        if (state.isVisible) {
            onActiveClick?.let { onClick ->
                TangemButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = resourceReference(R.string.prediction_positions_view_all_active),
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X9,
                    onClick = onClick,
                )
            }
        }
    }
}