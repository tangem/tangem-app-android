package com.tangem.features.polymarket.impl.history.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.history.ui.preview.ActivityRowPreviewProvider
import kotlin.math.ceil

@Composable
internal fun PolymarketHistorySkeleton(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        BoxWithConstraints(modifier = modifier.fillMaxSize().padding(contentPadding).clearAndSetSemantics {}) {
            val rowHeight = with(LocalDensity.current) {
                (TangemTheme.typography3.body.medium.lineHeight.toDp() +
                    TangemTheme.typography3.caption.medium.lineHeight.toDp() + 2.dp)
                    .coerceAtLeast(40.dp) + 32.dp
            }
            val rowCount = ceil(maxHeight / rowHeight).toInt().coerceAtLeast(1)
            LazyColumn(modifier = Modifier.fillMaxSize(), userScrollEnabled = false) {
                items(count = rowCount, key = { it }) { index ->
                    if (index % ROWS_PER_DATE == 0) ActivityDateSkeleton()
                    PolymarketActivityRowSkeleton()
                }
            }
        }
    }
}

@Composable
internal fun PolymarketActivityRowSkeleton(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { TangemShimmer(modifier = Modifier.size(40.dp), radius = 20.dp) },
        titleSlot = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TangemRow(
                    includeInnerPaddings = false,
                    contentLead = TangemRowContentLead.Start,
                    titleSlot = {
                        ActivityTextSkeleton(TangemTheme.typography3.body.medium, Modifier.width(88.dp))
                    },
                    valueSlot = {
                        ActivityTextSkeleton(TangemTheme.typography3.body.medium, Modifier.width(72.dp))
                    },
                )
                ActivityTextSkeleton(TangemTheme.typography3.caption.medium, Modifier.width(184.dp))
            }
        },
    )
}

@Composable
private fun ActivityDateSkeleton() {
    ActivityTextSkeleton(
        style = TangemTheme.typography3.subheading.medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp).width(96.dp),
    )
}

@Composable
private fun ActivityTextSkeleton(style: TextStyle, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        modifier = modifier.height(lineHeight).padding(vertical = 2.dp),
        radius = 8.dp,
    )
}

private const val ROWS_PER_DATE = 3

@Preview(name = "Light", showBackground = true, widthDp = 390)
@Preview(name = "Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun PolymarketActivityRowSkeletonPreview() {
    TangemThemePreviewRedesign {
        ProvideTangemShimmer {
            Column(modifier = Modifier.fillMaxWidth().background(TangemTheme.colors3.bg.primary)) {
                ActivityDateSkeleton()
                PolymarketActivityRowSkeleton()
                PolymarketActivityRow(state = ActivityRowPreviewProvider().values.first().state)
            }
        }
    }
}