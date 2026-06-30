package com.tangem.features.tangempay.tiers.current

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan

internal class TangemPayCurrentPlanComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCurrentPlanModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        TangemPayCurrentPlanScreen(state = state, modifier = modifier)
    }

    data class Params(val tariffPlan: TangemPayCustomerTariffPlan)
}