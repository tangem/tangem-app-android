package com.tangem.features.polymarket.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.main.model.PolymarketMainBlocksRefresh
import com.tangem.features.polymarket.impl.main.model.PolymarketPositionsPreviewModel
import com.tangem.features.polymarket.impl.main.ui.PolymarketPositionsPreview

/** "My predictions" block of the main screen: the first open positions and the ways into the full list. */
internal class PolymarketPositionsPreviewComponent(
    context: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: PolymarketPositionsPreviewModel = getOrCreateModel(params = params)

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PolymarketPositionsPreview(
            state = state,
            modifier = modifier,
            onActiveClick = model::onActiveClick,
            onFinishedClick = model::onFinishedClick,
        )
    }

    data class Params(
        val userWalletId: UserWalletId,
        val blocksRefresh: PolymarketMainBlocksRefresh,
    )

    companion object {
        const val KEY = "positions_preview"
    }
}