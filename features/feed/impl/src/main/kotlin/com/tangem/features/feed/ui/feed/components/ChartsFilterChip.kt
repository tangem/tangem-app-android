package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.model.market.list.state.SortByTypeUM

@Composable
internal fun ChartsFilterChip(sortByTypeUM: SortByTypeUM, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(12.dp))
            .background(
                color = if (isSelected) {
                    TangemTheme.colors.button.primary
                } else {
                    TangemTheme.colors.button.secondary
                },
            )
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 8.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sortByTypeUM.text.resolveReference(),
            style = TangemTheme.typography.button,
            color = if (isSelected) {
                TangemTheme.colors.text.primary2
            } else {
                TangemTheme.colors.text.primary1
            },
        )
    }
}