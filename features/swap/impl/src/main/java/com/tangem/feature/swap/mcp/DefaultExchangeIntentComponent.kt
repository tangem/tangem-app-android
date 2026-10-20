package com.tangem.feature.swap.mcp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.feature.swap.mcp.model.ExchangeIntentModel
import com.tangem.feature.swap.mcp.model.ExchangeIntentUM
import com.tangem.feature.swap.mcp.ui.ExchangeIntentModal
import com.tangem.features.swap.mcp.ExchangeIntentComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultExchangeIntentComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ExchangeIntentComponent.Params,
) : ExchangeIntentComponent, AppComponentContext by appComponentContext {

    private val model: ExchangeIntentModel = getOrCreateModel(params)

    private val offerSlot = childSlot(
        key = OFFER_SLOT_KEY,
        source = model.offerSlotNavigation,
        serializer = null,
        initialConfiguration = { model.shownOffer },
        handleBackButton = false,
        childFactory = { offer, factoryContext ->
            OfferSheetComponent(context = childByContext(factoryContext), offer = offer, onDismiss = model::onClose)
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val offerSlotState by offerSlot.subscribeAsState()
        offerSlotState.child?.instance?.Content(modifier = modifier)
    }

    private class OfferSheetComponent(
        context: AppComponentContext,
        private val offer: ExchangeIntentUM,
        private val onDismiss: () -> Unit,
    ) : ComposableContentComponent, AppComponentContext by context {

        @Composable
        override fun Content(modifier: Modifier) {
            ExchangeIntentModal(state = offer, onDismiss = onDismiss)
        }
    }

    @AssistedFactory
    interface Factory : ExchangeIntentComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ExchangeIntentComponent.Params,
        ): DefaultExchangeIntentComponent
    }

    private companion object {
        const val OFFER_SLOT_KEY = "offerSlot"
    }
}