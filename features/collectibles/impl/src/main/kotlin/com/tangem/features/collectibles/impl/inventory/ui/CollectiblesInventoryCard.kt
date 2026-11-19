package com.tangem.features.collectibles.impl.inventory.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.common.ui.CollectiblesPackImage
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM
import com.tangem.features.collectibles.impl.inventory.ui.preview.CollectiblesInventoryPreviewData

internal const val INVENTORY_CARD_ASPECT_RATIO = 179f / 226f

private val CARD_SHAPE = RoundedCornerShape(20.dp)

@Composable
internal fun CollectiblesInventoryCard(state: CollectiblesInventoryUM.ItemUM.Card, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickableSingle(onClick = state.onClick),
    ) {
        CardSlab(state = state)
        SpacerH(8.dp)
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerH(2.dp)
        Text(
            text = state.price.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun CardSlab(state: CollectiblesInventoryUM.ItemUM.Card, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio = INVENTORY_CARD_ASPECT_RATIO)
            .clip(CARD_SHAPE)
            .background(TangemTheme.colors3.bg.secondary)
            .border(width = 1.dp, color = TangemTheme.colors3.border.primary, shape = CARD_SHAPE),
    ) {
        CollectiblesPackImage(
            imageUrl = state.imageUrl,
            glowRadius = 24.dp,
            modifier = Modifier
                .matchParentSize()
                .padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 20.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TangemBadge(text = state.grade, size = TangemBadge.Size.X4)
            Spacer(modifier = Modifier.weight(1f))
            if (state.timeLeft != null) {
                TangemBadge(text = state.timeLeft, size = TangemBadge.Size.X4, status = TangemBadge.Status.Warning)
            }
        }
    }
}

@Preview(widthDp = 180, showBackground = true)
@Preview(widthDp = 180, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesInventoryCardPreview(
    @PreviewParameter(CollectiblesInventoryCardPreviewProvider::class) state: CollectiblesInventoryUM.ItemUM.Card,
) {
    TangemThemePreviewRedesign {
        CollectiblesInventoryCard(state = state)
    }
}

private class CollectiblesInventoryCardPreviewProvider :
    PreviewParameterProvider<CollectiblesInventoryUM.ItemUM.Card> {

    override val values = sequenceOf(
        CollectiblesInventoryPreviewData.expiredCard,
        CollectiblesInventoryPreviewData.underMinuteCard,
        CollectiblesInventoryPreviewData.minutesCard,
        CollectiblesInventoryPreviewData.hoursCard,
        CollectiblesInventoryPreviewData.collectionCard,
    )
}