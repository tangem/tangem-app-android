package com.tangem.features.collectibles.impl.confirmorder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.confirmorder.model.CollectiblesConfirmOrderModel
import com.tangem.features.collectibles.impl.confirmorder.ui.CollectiblesConfirmOrderScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Order, reveal video and card share one route: a route boundary would cut the pack's throw in half. */
internal class CollectiblesConfirmOrderComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesConfirmOrderModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        CollectiblesConfirmOrderScreen(state = state, modifier = modifier)
    }

    /** @property externalId the machine's public id (e.g. `pokemon_50`), not its internal id */
    data class Params(val userWalletId: UserWalletId, val externalId: String, val isTurboEnabled: Boolean)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesConfirmOrderComponent
    }
}