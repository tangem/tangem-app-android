package com.tangem.features.polymarket.impl.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.details.model.PolymarketEventDetailsModel
import com.tangem.features.polymarket.impl.details.ui.PolymarketEventDetailsScreen
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionConfig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Event details screen, pushed onto the feature stack over the Discovery feed.
 *

 * factory — its model is resolved from the model map by [getOrCreateModel].
 *
 * Hosts a nested `childSlot` for the Place-prediction bottom sheet, activated by the model's sheet
 * requests (an outcome tap on a market card).
 */
internal class PolymarketEventDetailsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketEventDetailsModel = getOrCreateModel(params = params)

    private val slotNavigation = SlotNavigation<PlacePredictionConfig>()

    private val placePredictionSlot = childSlot(
        key = "polymarketPlacePredictionSlot",
        source = slotNavigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { config, factoryContext ->
            createPlacePredictionSheet(
                config = config,
                factoryContext = childByContext(componentContext = factoryContext),
            )
        },
    )

    init {
        model.sheetRequests
            .onEach(slotNavigation::activate)
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val slotState by placePredictionSlot.subscribeAsState()

        PolymarketEventDetailsScreen(
            state = state,
            onBackClick = model::onBackClick,
            modifier = modifier,
        )

        slotState.child?.instance?.BottomSheet()
    }

    private fun createPlacePredictionSheet(
        config: PlacePredictionConfig,
        factoryContext: AppComponentContext,
    ): ComposableBottomSheetComponent = PlacePredictionComponent(
        appComponentContext = factoryContext,
        params = PlacePredictionComponent.Params(
            eventId = config.eventId,
            marketId = config.marketId,
            side = config.side,
        ),
        onDismiss = { slotNavigation.dismiss() },
    )

    /**
     * @property eventId event to show
     * @property userWalletId wallet the feature was opened for. Carried for the balances and signing the
     *  place-prediction flow will need; nothing reads it yet.
     * @property marketId market preselected by the caller, e.g. by tapping an outcome on the feed card
     * @property assetId outcome preselected by the caller
     */
    data class Params(
        val eventId: String,
        val userWalletId: UserWalletId,
        val marketId: String? = null,
        val assetId: String? = null,
    )
}