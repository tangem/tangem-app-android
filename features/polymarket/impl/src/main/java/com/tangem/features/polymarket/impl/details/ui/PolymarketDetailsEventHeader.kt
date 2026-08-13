package com.tangem.features.polymarket.impl.details.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketSubcategoryTabUM
import kotlinx.collections.immutable.ImmutableList

/** The volume delta is a growth by construction, so the indicator always points up. */
private const val GROWTH_GLYPH = "▲"

private val SubcategoryShape = RoundedCornerShape(percent = 50)

/** The first item of the sheet: the event's icon, its question and the volume figures. */
@Composable
internal fun EventHeader(state: PolymarketEventDetailsUM.Content) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        EventIcon(iconUrl = state.iconUrl, size = 48.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = state.title.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.medium,
        )
        if (state.totalVolume != null || state.change24h != null) {
            Spacer(modifier = Modifier.height(8.dp))
            MetaRow(state = state)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MetaRow(state: PolymarketEventDetailsUM.Content) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.totalVolume?.let { totalVolume ->
            MetaEntry(
                label = stringResourceSafe(R.string.prediction_event_total_volume),
                value = totalVolume.resolveReference(),
                valueColor = TangemTheme.colors3.text.accent.yellow,
            )
        }
        state.change24h?.let { change24h ->
            MetaEntry(
                label = stringResourceSafe(R.string.prediction_details_24h_change),
                value = "$GROWTH_GLYPH ${change24h.resolveReference()}",
                valueColor = TangemTheme.colors3.text.accent.blue,
            )
        }
    }
}

@Composable
private fun MetaEntry(label: String, value: String, valueColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
        )
        Text(
            text = value,
            color = valueColor,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SubcategoryBar(subcategories: ImmutableList<PolymarketSubcategoryTabUM>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = subcategories, key = { it.id }) { tab ->
            SubcategoryTab(tab = tab)
        }
    }
}

@Composable
private fun SubcategoryTab(tab: PolymarketSubcategoryTabUM) {
    val background = if (tab.isSelected) {
        Modifier
            .clip(SubcategoryShape)
            .background(TangemTheme.colors3.bg.tertiary)
    } else {
        Modifier.clip(SubcategoryShape)
    }
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .then(background)
            .clickable(onClick = tab.onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tab.label,
            color = if (tab.isSelected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.subheading.medium,
            maxLines = 1,
        )
    }
}

/** Round event or market artwork, falling back to a plain circle while there is no image. */
@Composable
internal fun EventIcon(iconUrl: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.tertiary),
    ) {
        if (iconUrl != null) {
            AsyncImage(
                modifier = Modifier.matchParentSize(),
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}