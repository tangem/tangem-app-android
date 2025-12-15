package com.tangem.features.yield.supply.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.impl.main.model.YieldSupplyModel
import com.tangem.features.yield.supply.impl.main.ui.YieldSupplyBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldSupplyComponent.Params,
) : YieldSupplyComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val yieldSupplyUM by model.uiState.collectAsStateWithLifecycle()

        YieldSupplyBlockContent(yieldSupplyUM = yieldSupplyUM, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : YieldSupplyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyComponent.Params,
        ): DefaultYieldSupplyComponent
    }
}