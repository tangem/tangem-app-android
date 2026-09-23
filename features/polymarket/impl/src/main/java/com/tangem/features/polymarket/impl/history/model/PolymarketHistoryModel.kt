package com.tangem.features.polymarket.impl.history.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class PolymarketHistoryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    // History stays empty until the activity API is connected.
    val uiState = PolymarketHistoryUM(
        content = PolymarketHistoryUM.Content.Empty(onExploreEventsClick = ::onBackClick),
        onBackClick = ::onBackClick,
    )

    private fun onBackClick() {
        router.pop()
    }
}