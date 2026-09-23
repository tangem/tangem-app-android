package com.tangem.features.polymarket.impl.placeprediction.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.ui.PlacePredictionStatusContent

internal class PlacePredictionStatusComponent(
    appComponentContext: AppComponentContext,
    private val params: PlacePredictionRoute.Status,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        PlacePredictionStatusContent(
            result = params.result,
            market = params.market,
            side = params.side,
            onToMainClick = { router.popTo(PolymarketRoute.Main(userWalletId = params.userWalletId)) },
            modifier = modifier,
        )
    }
}