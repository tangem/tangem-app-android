package com.tangem.features.feed.search.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.search.FeedSearchBarController
import com.tangem.features.feed.search.ui.state.FeedSearchUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@ModelScoped
internal class FeedSearchModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    searchBarController: FeedSearchBarController,
) : Model() {

    private val params = paramsContainer.require<FeedRoute.Search>()

    val uiState: StateFlow<FeedSearchUM>
        field = MutableStateFlow(FeedSearchUM(source = params.source))

    init {
        // the query is typed into the host's search bar, not into this screen
        searchBarController.state
            .map { it.query }
            .distinctUntilChanged()
            .onEach { query -> uiState.update { it.copy(query = query) } }
            .launchIn(modelScope)
    }
}