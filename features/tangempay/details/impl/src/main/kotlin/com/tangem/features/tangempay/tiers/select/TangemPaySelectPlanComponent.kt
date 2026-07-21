package com.tangem.features.tangempay.tiers.select

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.wallet.UserWalletId

internal class TangemPaySelectPlanComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPaySelectPlanModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler { model.onBackClick() }

        val state by model.state.collectAsStateWithLifecycle()
        TangemPaySelectPlanScreen(state = state, modifier = modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val tariffPlan: TangemPayCustomerTariffPlan,
        val source: TangemPaySelectPlanSource,
    )
}