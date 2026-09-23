package com.tangem.features.tangempay.orderCard.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardIntent
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardSuccessModel
import com.tangem.features.tangempay.orderCard.impl.ui.TangemPayOrderCardSuccessScreen

internal class TangemPayOrderCardSuccessComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayOrderCardSuccessModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        BackHandler(onBack = state.onFinishClick)
        TangemPayOrderCardSuccessScreen(state = state, modifier = modifier)
    }

    data class Params(
        val deliveryEtaMaxBusinessDays: Int,
        val email: String,
        val intent: TangemPayOrderCardIntent,
        val onFinish: () -> Unit,
    )
}