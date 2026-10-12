package com.tangem.features.polymarket.impl.history.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.history.ui.PolymarketHistoryScreen
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.Content
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.DateGroup
import kotlinx.collections.immutable.toImmutableList

@Preview(name = "Light", widthDp = 390, heightDp = 800)
@Preview(name = "Dark", widthDp = 390, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PolymarketHistoryScreenPreview(@PreviewParameter(HistoryPreviewProvider::class) case: HistoryPreviewCase) {
    TangemThemePreviewRedesign {
        val content = when (case) {
            HistoryPreviewCase.Empty -> Content.Empty(onExploreEventsClick = {})
            HistoryPreviewCase.Loading -> Content.Loading
            HistoryPreviewCase.Error -> Content.Error(onRetry = {})
            HistoryPreviewCase.Data -> previewHistoryContent(groupCount = 2)
            HistoryPreviewCase.LongList -> previewHistoryContent(groupCount = 10)
        }
        PolymarketHistoryScreen(state = PolymarketHistoryUM(content = content, onBackClick = {}))
    }
}

internal enum class HistoryPreviewCase { Empty, Loading, Error, Data, LongList }

internal class HistoryPreviewProvider : CollectionPreviewParameterProvider<HistoryPreviewCase>(
    HistoryPreviewCase.entries,
)

@Suppress("MagicNumber")
@Composable
private fun previewHistoryContent(groupCount: Int): Content.Data {
    var query by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(R.string.common_all) }
    val iconUrl = "android.resource://${LocalContext.current.packageName}/${R.drawable.img_eth_22}"
    val rows = ActivityRowPreviewProvider().values.take(6).map { it.state }.toList()
    return Content.Data(
        groups = List(groupCount) { group ->
            DateGroup(
                id = "date_$group",
                title = if (group == 0) {
                    resourceReference(R.string.common_today)
                } else {
                    stringReference("June ${12 - group}, 2026")
                },
                activities = rows.map { row ->
                    row.copy(id = "${group}_${row.id}", iconUrl = iconUrl)
                }.toImmutableList(),
            )
        }.toImmutableList(),
        tabs = listOf(
            R.string.common_all,
            R.string.prediction_history_bought,
            R.string.prediction_history_sold,
            R.string.prediction_history_funds_added,
            R.string.transaction_history_withdrawn,
        ).map { title ->
            TangemTabItemUM.Content(
                id = title.toString(),
                label = resourceReference(title),
                isSelected = selectedTab == title,
                onClick = { selectedTab = title },
            )
        }.toImmutableList(),
        search = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = query,
            onQueryChange = { query = it },
            isActive = isSearchActive,
            onActiveChange = { isSearchActive = it },
            onCloseClick = {
                query = ""
                isSearchActive = false
            },
        ),
        onDownloadCsvClick = {},
    )
}