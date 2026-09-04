package com.tangem.features.tangempay.eligibility

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.eligibility.country.ResidencyListModel
import com.tangem.features.tangempay.eligibility.country.ResidencyListScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayEligibilityComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: TangemPayEligibilityComponent.Params,
) : TangemPayEligibilityComponent, AppComponentContext by context {

    private val model: ResidencyListModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ResidencyListScreen(state = state, modifier = modifier)
        BackHandler(onBack = state.onBackClick ?: state.onCloseClick)
    }

    @AssistedFactory
    interface Factory : TangemPayEligibilityComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayEligibilityComponent.Params,
        ): DefaultTangemPayEligibilityComponent
    }
}