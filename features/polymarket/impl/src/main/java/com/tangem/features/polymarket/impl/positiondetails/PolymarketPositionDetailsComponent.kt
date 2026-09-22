package com.tangem.features.polymarket.impl.positiondetails

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
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.features.polymarket.impl.positiondetails.model.PolymarketPositionDetailsModel
import com.tangem.features.polymarket.impl.positiondetails.ui.PolymarketPositionDetailsScreen

internal class PolymarketPositionDetailsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {
    private val model: PolymarketPositionDetailsModel = getOrCreateModel(params = params)

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        PolymarketPositionDetailsScreen(state = state, modifier = modifier)
    }

    data class Params(val userWalletId: UserWalletId, val positionId: PolymarketPositionId)
}