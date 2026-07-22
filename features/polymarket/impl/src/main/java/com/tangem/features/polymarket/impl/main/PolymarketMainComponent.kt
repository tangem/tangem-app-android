package com.tangem.features.polymarket.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.main.ui.PolymarketMainScreen

/**
 * Discovery feed screen.
 *

 * factory — its model is resolved from the model map by [getOrCreateModel].
 */
internal class PolymarketMainComponent(
    appComponentContext: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketMainModel = getOrCreateModel()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PolymarketMainScreen(state = state, modifier = modifier)
    }
}