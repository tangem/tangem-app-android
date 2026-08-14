package com.tangem.features.polymarket.impl.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.details.model.PolymarketEventDetailsModel
import com.tangem.features.polymarket.impl.details.ui.PolymarketEventDetailsScreen
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionConfig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Event details, presented as a full-height modal bottom sheet over the Discovery feed (per design).
 *
 * Hosts a nested `childSlot` for the Place-prediction bottom sheet, activated by the model's sheet
 * requests (an outcome tap on a market card).
 */
internal class PolymarketEventDetailsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

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

    override fun dismiss() {
        model.onCloseClick()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val slotState by placePredictionSlot.subscribeAsState()

        val config = remember(this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = config,
            containerColor = TangemTheme.colors3.bg.primary,
            content = {
                PolymarketEventDetailsScreen(
                    state = state,
                    onCloseClick = model::onCloseClick,
                )
            },
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