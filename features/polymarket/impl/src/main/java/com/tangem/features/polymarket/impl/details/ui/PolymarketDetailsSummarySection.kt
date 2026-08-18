package com.tangem.features.polymarket.impl.details.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM

private const val KEY_DESCRIPTION = "description"
private const val KEY_INFO = "info"

private const val COLLAPSED_DESCRIPTION_LINES = 4

/** The tail of the feed: the resolution criteria and the event dates. */
internal fun LazyListScope.summarySection(state: PolymarketEventDetailsUM.Content) {
    state.description?.let { description ->
        item(key = KEY_DESCRIPTION) {
            DescriptionBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                description = description,
                isExpanded = state.isDescriptionExpanded,
                onReadMoreClick = state.onReadMoreClick,
            )
        }
    }

    if (state.resolutionDate != null || state.marketOpenedDate != null) {
        item(key = KEY_INFO) {
            InfoSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = state,
            )
        }
    }
}

@Composable
private fun InfoSection(state: PolymarketEventDetailsUM.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        state.resolutionDate?.let { resolutionDate ->
            InfoRow(
                label = stringResourceSafe(R.string.prediction_details_resolution_date),
                value = resolutionDate.resolveReference(),
            )
        }
        if (state.resolutionDate != null && state.marketOpenedDate != null) {
            HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
        }
        state.marketOpenedDate?.let { marketOpenedDate ->
            InfoRow(
                label = stringResourceSafe(R.string.prediction_details_market_opened),
                value = marketOpenedDate.resolveReference(),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.body.medium,
        )
        Text(
            text = value,
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.body.medium,
        )
    }
}

@Composable
private fun DescriptionBlock(
    description: TextReference,
    isExpanded: Boolean,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isClipped by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = description.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
            maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { isClipped = it.hasVisualOverflow },
        )
        if (!isExpanded && isClipped) {
            Text(
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onReadMoreClick)
                    .padding(top = 4.dp),
                text = stringResourceSafe(R.string.common_read_more),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.body.medium,
            )
        }
    }
}