package com.tangem.features.tangempay.orderCard.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardDataModel
import com.tangem.features.tangempay.orderCard.impl.ui.TangemPayOrderCardDataScreen

internal class TangemPayOrderCardDataComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayOrderCardDataModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        BackHandler(onBack = state.onBackClick)
        TangemPayOrderCardDataScreen(state = state, modifier = modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val onOrderSubmitted: (PlasticCardOrder) -> Unit,
        val onClose: () -> Unit,
    )
}