package com.tangem.features.polymarket.impl.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionConfig

/**
 * Placeholder event-details screen. Real UI arrives in a later task.
 *
 * Hosts a `childSlot` for the Place-prediction bottom sheet; its [SlotNavigation] lives directly in the component
 * since this stub has no dedicated model yet.
 *
 * @param userWalletId wallet the feature was opened for. Carried for the real screen, which will need it for
 *  balances and signing. Nothing reads it yet — this stub has no model.
 */
internal class PolymarketEventDetailsComponent(
    appComponentContext: AppComponentContext,
    private val eventId: String,
    @Suppress("UnusedPrivateProperty") private val userWalletId: UserWalletId,
    private val marketId: String? = null,
    private val assetId: String? = null,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val slotNavigation = SlotNavigation<PlacePredictionConfig>()

    private val bottomSheetSlot = childSlot(
        key = "polymarketPlacePredictionSlot",
        source = slotNavigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { config, factoryContext ->
            createBottomSheet(
                config = config,
                factoryContext = childByContext(componentContext = factoryContext),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val slotState by bottomSheetSlot.subscribeAsState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Event details: $eventId")
            if (marketId != null) {
                Text(text = "Preselected: $marketId / $assetId")
            }
            Text(
                text = "Place prediction",
                modifier = Modifier.clickable(onClick = ::onPlacePrediction),
            )
        }

        slotState.child?.instance?.BottomSheet()
    }

    private fun onPlacePrediction() {
        slotNavigation.activate(
            PlacePredictionConfig(eventId = eventId, marketId = marketId, side = assetId),
        )
    }

    private fun createBottomSheet(
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
}