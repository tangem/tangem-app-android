package com.tangem.features.polymarket.impl.fasttrading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.fasttrading.model.PolymarketFastTradingSettingsModel
import com.tangem.features.polymarket.impl.fasttrading.ui.PolymarketFastTradingSettingsScreen

internal class PolymarketFastTradingSettingsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketFastTradingSettingsModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        PolymarketFastTradingSettingsScreen(state = state, modifier = modifier)
    }

    data class Params(val userWalletId: UserWalletId)
}