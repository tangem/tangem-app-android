package com.tangem.features.tangempay.verified

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.tangempay.components.TangemPayVerifiedComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayVerifiedComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: TangemPayVerifiedComponent.Params,
) : TangemPayVerifiedComponent, AppComponentContext by context {

    private val model: TangemPayVerifiedModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemThemeRedesign {
            TangemPayVerifiedScreen(
                state = state,
                animated = true,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayVerifiedComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayVerifiedComponent.Params,
        ): DefaultTangemPayVerifiedComponent
    }
}