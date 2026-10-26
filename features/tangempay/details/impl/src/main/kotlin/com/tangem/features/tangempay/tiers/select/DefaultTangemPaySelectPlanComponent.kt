package com.tangem.features.tangempay.tiers.select

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.components.TangemPaySelectPlanComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPaySelectPlanComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPaySelectPlanComponent.Params,
) : TangemPaySelectPlanComponent, AppComponentContext by appComponentContext {

    private val model: TangemPaySelectPlanModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler { model.onBackClick() }

        val state by model.state.collectAsStateWithLifecycle()
        TangemPaySelectPlanScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TangemPaySelectPlanComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPaySelectPlanComponent.Params,
        ): DefaultTangemPaySelectPlanComponent
    }
}