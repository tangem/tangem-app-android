package com.tangem.features.staking.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.presentation.model.StakingModel
import com.tangem.features.staking.impl.presentation.ui.StakingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultStakingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: StakingComponent.Params,
) : StakingComponent, AppComponentContext by appComponentContext {

    private val model: StakingModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val currentState by model.uiState.collectAsStateWithLifecycle()
        StakingScreen(currentState)
    }

    @AssistedFactory
    interface Factory : StakingComponent.Factory {
        override fun create(context: AppComponentContext, params: StakingComponent.Params): DefaultStakingComponent
    }
}