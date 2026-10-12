package com.tangem.features.polymarket.impl.positions

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
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.features.polymarket.impl.positions.model.PolymarketPositionsModel
import com.tangem.features.polymarket.impl.positions.ui.PolymarketPositionsScreen

internal class PolymarketPositionsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketPositionsModel = getOrCreateModel(params = params)

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        PolymarketPositionsScreen(state = state, modifier = modifier)
    }

    data class Params(val userWalletId: UserWalletId, val initialTab: PolymarketPositionsTab)
}