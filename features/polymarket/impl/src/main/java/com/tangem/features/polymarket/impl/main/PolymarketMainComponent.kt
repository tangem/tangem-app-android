package com.tangem.features.polymarket.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.main.ui.PolymarketMainScreen

/**
 * Discovery feed screen.
 *

 * factory — its model is resolved from the model map by [getOrCreateModel].
 */
internal class PolymarketMainComponent(
    appComponentContext: AppComponentContext,
    private val accessMode: PolymarketAccessMode,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketMainModel = getOrCreateModel(params = accessMode)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PolymarketMainScreen(state = state, accessMode = accessMode, modifier = modifier)
    }
}