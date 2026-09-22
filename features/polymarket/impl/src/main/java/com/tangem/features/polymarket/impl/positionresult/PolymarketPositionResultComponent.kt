package com.tangem.features.polymarket.impl.positionresult

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.features.polymarket.impl.positionresult.model.PolymarketPositionResultModel
import com.tangem.features.polymarket.impl.positionresult.ui.PolymarketPositionResultScreen

internal class PolymarketPositionResultComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketPositionResultModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        PolymarketPositionResultScreen(
            state = state,
            onActionClick = model::onActionClick,
            modifier = modifier,
        )
    }

    data class Params(val userWalletId: UserWalletId, val position: PolymarketPosition.Open)
}