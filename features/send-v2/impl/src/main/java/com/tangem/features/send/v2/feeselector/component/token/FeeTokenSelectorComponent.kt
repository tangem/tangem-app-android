package com.tangem.features.send.v2.feeselector.component.token

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.token.model.FeeTokenSelectorModel
import com.tangem.features.send.v2.feeselector.component.token.ui.FeeTokenSelectorContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class FeeTokenSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: FeeSelectorComponentParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: FeeTokenSelectorModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        FeeTokenSelectorContent(
            state = state,
            intents = model,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            appComponentContext: AppComponentContext,
            params: FeeSelectorComponentParams,
        ): FeeTokenSelectorComponent
    }
}