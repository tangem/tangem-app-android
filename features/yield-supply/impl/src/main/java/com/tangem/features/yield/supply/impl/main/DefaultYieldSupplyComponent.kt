package com.tangem.features.yield.supply.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.earn.EarnBlock
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.impl.main.model.YieldSupplyModel
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
        val earnBlockUM by model.uiState.collectAsStateWithLifecycle()
        earnBlockUM?.let { blockUM ->
            val tag = if (blockUM is EarnBlockUM.Content &&
                blockUM.backgroundUM is EarnBlockUM.BackgroundUM.AccentSoft
            ) {
                TokenDetailsScreenTestTags.YIELD_SUPPLY_AVAILABLE_BLOCK
            } else {
                TokenDetailsScreenTestTags.YIELD_SUPPLY_BLOCK
            }
            EarnBlock(state = blockUM, modifier = modifier.testTag(tag))
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