package com.tangem.features.promobanners.impl.campaigns.v2.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoBlockUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoLinkUM
import kotlinx.collections.immutable.ImmutableList

private const val TILES_PER_ROW = 2

@Composable
internal fun PromoTextBlock(block: PromoBlockUM.Text, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (block.title != null) {
            Text(
                text = block.title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        Text(
            text = textWithLink(text = block.text, link = block.link),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** Tiles go two per row; an odd last tile takes the full width. */
@Composable
internal fun PromoTilesBlock(block: PromoBlockUM.Tiles, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (block.title != null) {
            Text(
                text = block.title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            block.tiles.chunked(TILES_PER_ROW).forEach { rowTiles ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTiles.forEach { tile ->
                        PromoTile(tile = tile, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoTile(tile: PromoBlockUM.Tile, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.opaque.primary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = tile.label.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = tile.value.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
internal fun PromoTableBlock(block: PromoBlockUM.Table, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (block.title != null) {
            Text(
                text = block.title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        if (block.headers.isNotEmpty()) {
            PromoTableRow(cells = block.headers, isHeader = true, hasDivider = false)
        }
        block.rows.forEachIndexed { index, cells ->
            PromoTableRow(cells = cells, isHeader = false, hasDivider = index < block.rows.lastIndex)
        }
    }
}

@Composable
private fun PromoTableRow(cells: ImmutableList<TextReference>, isHeader: Boolean, hasDivider: Boolean) {
    val label = cells.firstOrNull() ?: return
    val labelRole = if (isHeader) TangemRowTextRole.Subtitle else TangemRowTextRole.Title
    val valueRole = if (isHeader) TangemRowTextRole.Subvalue else TangemRowTextRole.Value

    TangemRow(
        divider = hasDivider,
        contentLead = TangemRowContentLead.End,
        titleSlot = { TangemRowText(text = label, role = labelRole) },
        valueSlot = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.drop(1).forEach { value -> TangemRowText(text = value, role = valueRole) }
            }
        },
    )
}

@Composable
internal fun PromoHighlightBlock(block: PromoBlockUM.Highlight, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        title = block.title,
        description = block.description,
        variant = TangemMessageBanner.Variant.Info,
        showGlowRing = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun textWithLink(text: TextReference, link: PromoLinkUM?) = buildAnnotatedString {
    append(text.resolveReference())
    if (link == null) return@buildAnnotatedString

    append(" ")
    withLink(LinkAnnotation.Clickable(tag = "PROMO_LINK", linkInteractionListener = { link.onClick() })) {
        withStyle(SpanStyle(color = TangemTheme.colors3.text.primary)) {
            append(link.text.resolveReference())
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoCampaignBlocks() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PromoTilesBlock(PromoCampaignPreviewData.evenTiles)
            PromoTableBlock(PromoCampaignPreviewData.tableWithHeaders)
            PromoHighlightBlock(PromoCampaignPreviewData.highlightTitleOnly)
        }
    }
}
// endregion