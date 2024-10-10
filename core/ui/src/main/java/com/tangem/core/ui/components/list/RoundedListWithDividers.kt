package com.tangem.core.ui.components.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.rows.RoundableCornersRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val ROUNDED_LIST_WITH_DIVIDERS_HEADER_KEY = "ROUNDED_LIST_WITH_DIVIDERS_HEADER_KEY"
private const val ROUNDED_LIST_WITH_DIVIDERS_FOOTER_KEY = "ROUNDED_LIST_WITH_DIVIDERS_FOOTER_KEY"

@Composable
fun RoundedListWithDividers(
    rows: ImmutableList<RoundedListWithDividersItemData>,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
) {
    LazyColumn(modifier = modifier) {
        this.roundedListWithDividersItems(
            rows = rows,
            headerContent = headerContent,
            footerContent = footerContent,
        )
    }
}

fun LazyListScope.roundedListWithDividersItems(
    rows: ImmutableList<RoundedListWithDividersItemData>,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    hideEndText: Boolean = false,
) {
    if (headerContent != null) {
        item(key = ROUNDED_LIST_WITH_DIVIDERS_HEADER_KEY) {
            headerContent()
        }
    }

    itemsIndexed(
        items = rows,
        key = { _, item -> item.id },
    ) { index, row ->
        InitialInfoContentRow(
            startText = row.startText.resolveReference(),
            endText = row.endText.orMaskWithStars(hideEndText && row.isEndTextHideable).resolveReference(),
            currentIndex = index,
            lastIndex = rows.lastIndex,
            iconClick = row.iconClick,
            endTextColor = if (row.isEndTextHighlighted) {
                TangemTheme.colors.text.accent
            } else {
                TangemTheme.colors.text.tertiary
            },
            showDivider = index < rows.lastIndex,
        )
    }

    if (footerContent != null) {
        item(key = ROUNDED_LIST_WITH_DIVIDERS_FOOTER_KEY) {
            footerContent()
        }
    }
}

@Composable
private fun InitialInfoContentRow(
    startText: String,
    endText: String,
    currentIndex: Int,
    lastIndex: Int,
    showDivider: Boolean,
    endTextColor: Color = TangemTheme.colors.text.tertiary,
    iconClick: (() -> Unit)? = null,
) {
    Box {
        RoundableCornersRow(
            startText = startText,
            startTextColor = TangemTheme.colors.text.primary1,
            startTextStyle = TangemTheme.typography.body2,
            endText = endText,
            endTextColor = endTextColor,
            endTextStyle = TangemTheme.typography.body2,
            currentIndex = currentIndex,
            lastIndex = lastIndex,
            iconResId = R.drawable.ic_information_24,
            iconClick = iconClick,
        )
        if (showDivider) {
            RoundedListDivider(
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
fun RoundedListDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = TangemTheme.dimens.spacing16)
            .fillMaxWidth()
            .height(TangemTheme.dimens.size0_5)
            .background(TangemTheme.colors.stroke.primary),
    )
}

data class RoundedListWithDividersItemData(
    val id: Int,
    val startText: TextReference,
    val endText: TextReference,
    val isEndTextHighlighted: Boolean = false,
    val iconClick: (() -> Unit)? = null,
    val isEndTextHideable: Boolean = false,
)

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_StakingInfoBottomSheet() {
    TangemThemePreview {
        RoundedListWithDividers(
            rows = persistentListOf(
                RoundedListWithDividersItemData(
                    id = 1,
                    startText = TextReference.Str("Key 1"),
                    endText = TextReference.Str("Value 1"),
                ),
                RoundedListWithDividersItemData(
                    id = 2,
                    startText = TextReference.Str("Key 2"),
                    endText = TextReference.Str("Value 2"),
                ),
                RoundedListWithDividersItemData(
                    id = 3,
                    startText = TextReference.Str("Key 3"),
                    endText = TextReference.Str("Value 3"),
                    iconClick = {},
                ),
            ),
        )
    }
}