package com.tangem.features.send.v2.subcomponents.destination

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.v2.send.SendRoute
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationModel
import com.tangem.features.send.v2.subcomponents.destination.ui.SendDestinationContent
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import kotlinx.coroutines.flow.Flow

internal class SendDestinationComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendDestinationModel = getOrCreateModel(params = params, router = router)

    fun updateState(state: DestinationUM) = model.updateState(state)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState.collectAsStateWithLifecycle()

        SendDestinationContent(state = state.value, model, isBalanceHidden = false)
    }

    data class Params(
        val state: DestinationUM,
        val analyticsCategoryName: String,
        val userWallet: UserWallet,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val callback: ModelCallback,
        val currentRoute: Flow<SendRoute.Destination>,
        val isEditMode: Boolean,
    )

    interface ModelCallback {
        fun onDestinationResult(destinationUM: DestinationUM)
    }
}