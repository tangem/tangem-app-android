package com.tangem.features.yield.supply.impl.warning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.yield.supply.api.YieldSupplyDepositedWarningComponent
import com.tangem.features.yield.supply.impl.warning.model.YieldSupplyDepositedWarningModel
import com.tangem.features.yield.supply.impl.warning.ui.YieldSupplyDepositedWarningContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyDepositedWarningComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldSupplyDepositedWarningComponent.Params,
) : AppComponentContext by appComponentContext, YieldSupplyDepositedWarningComponent {

    private val model: YieldSupplyDepositedWarningModel = getOrCreateModel(params = params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        YieldSupplyDepositedWarningContent(warningUM = state, onDismiss = params.onDismiss)
    }

    @AssistedFactory
    interface Factory : YieldSupplyDepositedWarningComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyDepositedWarningComponent.Params,
        ): DefaultYieldSupplyDepositedWarningComponent
    }
}