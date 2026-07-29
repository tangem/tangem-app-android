package com.tangem.core.ui.ds2.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Design-system v2 filter group — a horizontally scrollable row of [TangemFilterItem] chips.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/branch/0xt9Tg8x8f0Z0m2KUdLG9q/%F0%9F%92%A0-DS-Components?node-id=7959-24284&m=dev)
 *
 * @param items Chips to render, in display order.
 * @param modifier Modifier applied to the row. The row fills the available width by default.
 * @param variant Visual style applied to every chip (Figma `APPEARANCE`).
 *   See [TangemFilterItem.Variant].
 * @param contentPadding Padding around the chips, applied inside the scrollable area so the first
 *   and last chip can scroll under it.
 */
@Composable
fun TangemFilterGroup(
    items: ImmutableList<TangemFilterItemUM>,
    modifier: Modifier = Modifier,
    variant: TangemFilterItem.Variant = TangemFilterItem.Variant.Material,
    contentPadding: PaddingValues = PaddingValues(horizontal = HorizontalPadding),
) {
    ProvideTangemShimmer {
        LazyRow(
            modifier = modifier,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            items(items = items, key = { it.id }) { item ->
                TangemFilterItem(state = item, variant = variant)
            }
        }
    }
}

private val HorizontalPadding: Dp = 16.dp
private val ItemSpacing: Dp = 4.dp

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemFilterGroupPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TangemFilterItem.Variant.entries.forEach { variant ->
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = variant.name,
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.body.medium,
                )
                TangemFilterGroup(items = PreviewItems, variant = variant)
            }
        }
    }
}

private val PreviewItems: ImmutableList<TangemFilterItemUM> = persistentListOf(
    TangemFilterItemUM.Active(
        id = "network",
        value = stringReference("Value"),
        onClick = {},
        onClearClick = {},
    ),
    TangemFilterItemUM.Active(
        id = "token",
        value = stringReference("Value"),
        counter = 1,
        onClick = {},
        onClearClick = {},
    ),
    TangemFilterItemUM.Inactive(
        id = "period",
        label = stringReference("Label"),
        onClick = {},
    ),
    TangemFilterItemUM.Loading(id = "loading"),
)

// endregion