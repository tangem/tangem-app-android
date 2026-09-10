package com.tangem.features.tangempay.withdraw.hub

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.withdraw.hub.model.TangemPayWithdrawHubModel
import com.tangem.features.tangempay.withdraw.hub.ui.TangemPayWithdrawHubScreen

internal class TangemPayWithdrawHubComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayWithdrawHubModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = state.onBackClick)

        TangemPayWithdrawHubScreen(state = state, modifier = modifier)
    }

    data class Params(val userWalletId: UserWalletId)
}