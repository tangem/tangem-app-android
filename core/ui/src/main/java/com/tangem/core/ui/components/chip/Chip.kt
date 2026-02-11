package com.tangem.core.ui.components.chip

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun Chip(state: ChipUM, modifier: Modifier = Modifier) {
    val backgroundColor by animateColorAsState(
        targetValue = if (state.isSelected) {
            TangemTheme.colors.button.primary
        } else {
            TangemTheme.colors.button.secondary
        },
    )

    val textColor by animateColorAsState(
        targetValue = if (state.isSelected) {
            TangemTheme.colors.text.primary2
        } else {
            TangemTheme.colors.text.primary1
        },
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = state.onClick,
            )
            .padding(PaddingValues(horizontal = 24.dp, vertical = 8.dp)),
    ) {
        Text(
            text = state.text.resolveReference(),
            style = TangemTheme.typography.button,
            color = textColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChipPreview() {
    TangemThemePreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(16.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    state = ChipUM(
                        id = 0,
                        text = TextReference.Str("All News"),
                        isSelected = true,
                        onClick = {},
                    ),
                )
                Chip(
                    state = ChipUM(
                        id = 1,
                        text = TextReference.Str("Regulation"),
                        isSelected = false,
                        onClick = {},
                    ),
                )
                Chip(
                    state = ChipUM(
                        id = 2,
                        text = TextReference.Str("ETFs"),
                        isSelected = false,
                        onClick = {},
                    ),
                )
                Chip(
                    state = ChipUM(
                        id = 3,
                        text = TextReference.Str("Institutions"),
                        isSelected = false,
                        onClick = {},
                    ),
                )
            }
        }
    }
}