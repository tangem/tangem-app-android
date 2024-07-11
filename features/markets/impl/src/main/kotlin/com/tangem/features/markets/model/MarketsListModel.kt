package com.tangem.features.markets.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.ui.entity.ListUM
import com.tangem.features.markets.ui.entity.MarketsListUM
import com.tangem.features.markets.ui.entity.SortByTypeUM
import com.tangem.features.markets.ui.preview.MarketChartListItemPreviewDataProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class MarketsListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val state = MutableStateFlow(
        MarketsListUM(
            list = ListUM.Loading,
            searchBar = SearchBarUM(
                placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                query = "",
                onQueryChange = {},
                isActive = false,
                onActiveChange = { },
            ),
            selectedSortBy = SortByTypeUM.Rating,
            selectedInterval = MarketsListUM.TrendInterval.H24,
            onIntervalClick = {},
            onSortByButtonClick = {},
        ),
    )

    init {
        modelScope.launch {
            delay(timeMillis = 5000)
            state.update {
                it.copy(
                    list = ListUM.Content(
                        items = MarketChartListItemPreviewDataProvider().values
                            .flatMap { item -> List(size = 10) { item } }
                            .mapIndexed { index, item ->
                                item.copy(id = index.toString())
                            }
                            .toImmutableList(),
                    ),
                )
            }
        }
    }

    // TODO
}