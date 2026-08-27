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
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardTypeModel
import com.tangem.features.tangempay.orderCard.impl.ui.TangemPayOrderCardTypeScreen

internal class TangemPayOrderCardTypeComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayOrderCardTypeModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler { model.onBackClick() }

        val state by model.state.collectAsStateWithLifecycle()
        TangemPayOrderCardTypeScreen(state = state, modifier = modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val onSelectVirtual: () -> Unit,
        val onSelectPlastic: () -> Unit,
    )
}