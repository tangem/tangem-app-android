package com.tangem.core.ui.components.grid

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.tangem.core.ui.components.grid.entity.SeedPhraseGridItem
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SeedPhraseGridBlock(items: ImmutableList<SeedPhraseGridItem>, modifier: Modifier = Modifier) {
    VerticalGrid(
        modifier = modifier,
        items = items,
    ) { item ->
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.size8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
                Text(
                    modifier = Modifier.width(TangemTheme.dimens.size40),
                    text = "${item.index}.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
                Text(
                    text = item.mnemonic,
                    style = TangemTheme.typography.button,
                    color = TangemTheme.colors.text.primary1,
                )
            } else {
                Text(
                    text = item.mnemonic,
                    style = TangemTheme.typography.button,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    modifier = Modifier.width(TangemTheme.dimens.size40),
                    text = "${item.index}.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private inline fun <T> VerticalGrid(
    items: ImmutableList<T>,
    modifier: Modifier = Modifier,
    crossinline content: @Composable (T) -> Unit,
) {
    val columnLength = items.size / 2
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(2) { index ->
            Column {
                for (i in 0 until columnLength) {
                    val item = items[index * columnLength + i]
                    content(item)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        SeedPhraseGridBlock(
            items = List(12) {
                SeedPhraseGridItem(
                    index = it + 1,
                    mnemonic = "word${it + 1}",
                )
            }.toImmutableList(),
        )
    }
}