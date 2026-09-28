package com.tangem.features.polymarket.impl.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.features.polymarket.impl.main.PolymarketPositionsPreviewComponent
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.positiondetails.model.canOpenPositionDetails
import com.tangem.features.polymarket.impl.positionresult.model.canOpenPositionResult
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class PolymarketPositionsPreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val loader: PolymarketPositionsPreviewLoader,
) : Model() {

    private val params = paramsContainer.require<PolymarketPositionsPreviewComponent.Params>()

    val uiState: StateFlow<PolymarketPositionsPreviewUM> = loader.state

    init {
        params.blocksRefresh.register {
            loader.refresh(modelScope, params.userWalletId, ::openPosition)
        }
    }

    fun onResume() {
        loader.resume(modelScope, params.userWalletId, ::openPosition)
    }

    fun onPause() {
        loader.pause()
    }

    fun onActiveClick() {
        openPositions(PolymarketPositionsTab.ACTIVE)
    }

    fun onFinishedClick() {
        openPositions(PolymarketPositionsTab.FINISHED)
    }

    private fun openPositions(tab: PolymarketPositionsTab) {
        router.push(PolymarketRoute.Positions(userWalletId = params.userWalletId, initialTab = tab))
    }

    private fun openPosition(position: PolymarketPosition.Open) {
        when {
            position.canOpenPositionResult -> router.push(PolymarketRoute.PositionResult(params.userWalletId, position))
            position.canOpenPositionDetails -> router.push(
                PolymarketRoute.PositionDetails(params.userWalletId, position.id),
            )
        }
    }
}