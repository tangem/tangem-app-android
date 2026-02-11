package com.tangem.features.send.v2.feeselector.component.extended

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.extended.model.FeeExtendedSelectorModel
import com.tangem.features.send.v2.feeselector.component.extended.ui.FeeExtendedSelectorContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class FeeExtendedSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: FeeSelectorComponentParams,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: FeeExtendedSelectorModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        FeeExtendedSelectorContent(
            modifier = modifier,
            state = state,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            appComponentContext: AppComponentContext,
            params: FeeSelectorComponentParams,
        ): FeeExtendedSelectorComponent
    }
}