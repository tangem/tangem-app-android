package com.tangem.features.polymarket.impl.main.addfunds.network

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.features.polymarket.impl.main.addfunds.network.ui.PolymarketChooseNetworkBottomSheet

internal class PolymarketChooseNetworkComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketChooseNetworkModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        PolymarketChooseNetworkBottomSheet(state = model.uiState)
    }

    data class Params(
        val receiveConfigs: List<TokenReceiveConfig>,
        val onNetworkClick: (TokenReceiveConfig) -> Unit,
        val onDismiss: () -> Unit,
    )
}