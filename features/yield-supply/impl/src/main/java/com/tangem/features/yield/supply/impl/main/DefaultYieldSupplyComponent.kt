package com.tangem.features.yield.supply.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.earn.EarnBlock
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.impl.main.model.YieldSupplyModel
import com.tangem.features.yield.supply.impl.main.ui.YieldSupplyBlockContentLegacy
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
        if (LocalRedesignEnabled.current) {
            val earnBlockUM by model.uiState.collectAsStateWithLifecycle()
            earnBlockUM?.let { EarnBlock(state = it, modifier = modifier) }
        } else {
            val yieldSupplyUM by model.uiStateLegacy.collectAsStateWithLifecycle()
            YieldSupplyBlockContentLegacy(yieldSupplyUM = yieldSupplyUM, modifier = modifier)
        }
    }

    @AssistedFactory
    interface Factory : YieldSupplyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyComponent.Params,
        ): DefaultYieldSupplyComponent
    }
}