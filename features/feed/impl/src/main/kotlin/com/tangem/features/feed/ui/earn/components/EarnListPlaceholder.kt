package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun EarnItemPlaceholder(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
    ) {
        TangemShimmer(
            radius = 999.dp,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .padding(end = 8.dp)
                .size(40.dp),
        )
        EarnRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 72.dp,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_TOP)
                .padding(end = 8.dp),
        )
        EarnRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 44.dp,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_BOTTOM)
                .padding(end = 8.dp),
        )
        EarnRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 72.dp,
            modifier = Modifier.layoutId(TangemRowLayoutId.END_TOP),
        )
        EarnRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 44.dp,
            modifier = Modifier.layoutId(TangemRowLayoutId.END_BOTTOM),
        )
    }
}

@Composable
private fun EarnRowShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

private const val PLACEHOLDER_ITEMS_COUNT = 8

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListPlaceholderPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            repeat(PLACEHOLDER_ITEMS_COUNT) {
                EarnItemPlaceholder()
            }
        }
    }
}