package com.tangem.core.ui.components.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.rows.CornersToRound
import com.tangem.core.ui.components.rows.RoundableCornersRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RoundedListWithDividers(rows: List<RoundedListWithDividersItemData>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        this.roundedListItems(rows)
    }
}

fun LazyListScope.roundedListItems(rows: List<RoundedListWithDividersItemData>) {
    itemsIndexed(
        items = rows,
        key = { _, item -> item.id },
    ) { index, row ->
        InitialInfoContentRow(
            startText = row.startText.resolveReference(),
            endText = row.endText.resolveReference(),
            cornersToRound = getCornersToRound(index, rows.size),
            iconClick = row.iconClick,
        )
        if (index < rows.lastIndex) {
            RoundedListDivider()
        }
    }
}

@Composable
private fun InitialInfoContentRow(
    startText: String,
    endText: String,
    cornersToRound: CornersToRound,
    iconClick: (() -> Unit)? = null,
) {
    RoundableCornersRow(
        startText = startText,
        startTextColor = TangemTheme.colors.text.primary1,
        startTextStyle = TangemTheme.typography.body2,
        endText = endText,
        endTextColor = TangemTheme.colors.text.tertiary,
        endTextStyle = TangemTheme.typography.body2,
        cornersToRound = cornersToRound,
        iconResId = R.drawable.ic_information_24,
        iconClick = iconClick,
    )
}

@Composable
fun RoundedListDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size0_5),
    ) {
        Box(
            modifier = Modifier
                .width(TangemTheme.dimens.size16)
                .height(TangemTheme.dimens.size0_5)
                .background(TangemTheme.colors.background.primary),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(TangemTheme.dimens.size0_5)
                .background(TangemTheme.colors.background.tertiary),
        )
    }
}

private fun getCornersToRound(currentIndex: Int, listSize: Int): CornersToRound {
    return when (currentIndex) {
        0 -> CornersToRound.TOP_2
        listSize - 1 -> CornersToRound.BOTTOM_2
        else -> CornersToRound.ZERO
    }
}

data class RoundedListWithDividersItemData(
    val id: Int,
    val startText: TextReference,
    val endText: TextReference,
    val iconClick: (() -> Unit)? = null,
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
