package com.tangem.features.polymarket.impl.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.history.model.PolymarketHistoryModel
import com.tangem.features.polymarket.impl.history.ui.PolymarketHistoryScreen

internal class PolymarketHistoryComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketHistoryModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        PolymarketHistoryScreen(state = model.uiState, modifier = modifier)
    }

    data class Params(val userWalletId: UserWalletId)
}