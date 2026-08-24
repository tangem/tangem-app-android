package com.tangem.features.polymarket.impl.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.search.model.PolymarketSearchModel
import com.tangem.features.polymarket.impl.search.ui.PolymarketSearchScreen
import androidx.compose.ui.Modifier

/** Full-text search over discoverable events. */
internal class PolymarketSearchComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketSearchModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PolymarketSearchScreen(
            state = state,
            onLoadMore = model::onLoadMore,
            modifier = modifier,
        )
    }

    /**
     * @property userWalletId the wallet the feature runs for, carried into the details of a tapped result
     */
    data class Params(
        val userWalletId: UserWalletId,
    )
}