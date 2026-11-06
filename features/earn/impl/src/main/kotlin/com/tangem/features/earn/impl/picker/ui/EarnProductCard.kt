package com.tangem.features.earn.impl.picker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.features.earn.impl.picker.ui.state.EarnProductCardUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun EarnProductCards(products: ImmutableList<EarnProductCardUM>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(horizontal = PAGE_MARGIN, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        products.forEach { card -> EarnProductCard(card = card) }
    }
}

@Composable
private fun EarnProductCard(card: EarnProductCardUM, modifier: Modifier = Modifier) {
    val colors = TangemTheme.colors3
    val background = if (card.isSelected) colors.bg.inverse else colors.bg.transparent.primary
    val titleColor = if (card.isSelected) colors.text.inverse.secondary else colors.text.secondary
    val rateColor = if (card.isSelected) colors.text.inverse.primary else colors.text.primary

    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .selectable(selected = card.isSelected, role = Role.RadioButton, onClick = card.onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = card.title.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (card.isSelected) {
                Icon(
                    imageVector = Icons.ic_checkmark_16,
                    contentDescription = null,
                    tint = rateColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = card.rate.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = rateColor,
        )
    }
}

private val CARD_WIDTH = 173.dp