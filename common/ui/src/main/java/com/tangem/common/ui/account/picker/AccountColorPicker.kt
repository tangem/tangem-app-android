package com.tangem.common.ui.account.picker

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Colors per row, as in the account form design */
private const val COLORS_IN_ROW = 6

/** Swatch diameter; the selection ring is drawn around it inside [ItemSize] */
private val SwatchSize = 40.dp

/** Item box: fits the swatch plus the selection ring, and acts as the touch target */
private val ItemSize = 48.dp

/**
 * Grid of account colors, one of them selected.
 *
 * Renders the grid only — the caller supplies the surrounding card, so the same picker fits screens that decorate
 * their blocks differently.
 *
 * @param selectedColor color drawn with the selection ring
 * @param colors colors to offer, in the order they are shown
 * @param onColorClick invoked with the tapped color, including the already selected one
 * @param modifier applied to the grid container
 */
@Composable
fun AccountColorPicker(
    selectedColor: CryptoPortfolioIcon.Color,
    colors: ImmutableList<CryptoPortfolioIcon.Color>,
    onColorClick: (CryptoPortfolioIcon.Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = COLORS_IN_ROW,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.fastForEach { color ->
            ColorItem(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorClick(color) },
            )
        }
    }
}

@Composable
private fun ColorItem(color: CryptoPortfolioIcon.Color, isSelected: Boolean, onClick: () -> Unit) {
    val uiColor = color.getUiColor()

    Box(
        modifier = Modifier
            .size(ItemSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(ItemSize)
                    .border(width = 2.dp, color = uiColor, shape = CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .size(SwatchSize)
                .background(color = uiColor, shape = CircleShape),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AccountColorPicker() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(16.dp),
        ) {
            AccountColorPicker(
                selectedColor = CryptoPortfolioIcon.Color.SweetDesire,
                colors = CryptoPortfolioIcon.Color.entries.toImmutableList(),
                onColorClick = {},
            )
        }
    }
}