package com.tangem.features.tangempay.eligibility.country

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.eligibility.TangemPayCountrySelectorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayCountrySelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: TangemPayCountrySelectorComponent.Params,
) : TangemPayCountrySelectorComponent, AppComponentContext by context {

    private val model: CountrySelectorModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ResidencyListScreen(state = state, modifier = modifier)
        BackHandler(onBack = state.onBackClick ?: state.onCloseClick)
    }

    @AssistedFactory
    interface Factory : TangemPayCountrySelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayCountrySelectorComponent.Params,
        ): DefaultTangemPayCountrySelectorComponent
    }
}