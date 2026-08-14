package com.tangem.common.ui.account.picker

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.account.getResId
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Icons per row, as in the account form design */
private const val ICONS_IN_ROW = 6

/** Circle diameter; the selection ring is drawn around it inside [ItemSize] */
private val CircleSize = 40.dp

/** Item box: fits the circle plus the selection ring, and acts as the touch target */
private val ItemSize = 48.dp

/**
 * Grid of account icons, one of them selected.
 *
 * Renders the grid only — the caller supplies the surrounding card, so the same picker fits screens that decorate
 * their blocks differently.
 *
 * [CryptoPortfolioIcon.Icon.Letter] stands apart: it renders the account's initial rather than a glyph, so it is
 * tinted as an info accent instead of a neutral icon.
 *
 * @param selectedIcon icon drawn with the selection ring
 * @param icons icons to offer, in the order they are shown
 * @param onIconClick invoked with the tapped icon, including the already selected one
 * @param modifier applied to the grid container
 */
@Composable
fun AccountIconPicker(
    selectedIcon: CryptoPortfolioIcon.Icon,
    icons: ImmutableList<CryptoPortfolioIcon.Icon>,
    onIconClick: (CryptoPortfolioIcon.Icon) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = ICONS_IN_ROW,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icons.fastForEach { icon ->
            IconItem(
                icon = icon,
                isSelected = icon == selectedIcon,
                onClick = { onIconClick(icon) },
            )
        }
    }
}

@Composable
private fun IconItem(icon: CryptoPortfolioIcon.Icon, isSelected: Boolean, onClick: () -> Unit) {
    val isLetter = icon == CryptoPortfolioIcon.Icon.Letter

    val circleColor: Color = when {
        isLetter -> TangemTheme.colors3.bg.status.infoSubtle
        isSelected -> TangemTheme.colors3.bg.opaque.secondary
        else -> TangemTheme.colors3.bg.opaque.primary
    }
    val iconTint: Color = if (isLetter) {
        TangemTheme.colors3.icon.brand
    } else {
        TangemTheme.colors3.icon.secondary
    }

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
                    .border(width = 2.dp, color = TangemTheme.colors3.border.tertiary, shape = CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .size(CircleSize)
                .background(color = circleColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = ImageVector.vectorResource(id = icon.getResId()),
                contentDescription = null,
                tint = iconTint,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AccountIconPicker() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(16.dp),
        ) {
            AccountIconPicker(
                selectedIcon = CryptoPortfolioIcon.Icon.Family,
                icons = CryptoPortfolioIcon.Icon.entries.toImmutableList(),
                onIconClick = {},
            )
        }
    }
}