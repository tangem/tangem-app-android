package com.tangem.features.send.v2.feeselector.component.speed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.speed.model.FeeSpeedSelectorModel
import com.tangem.features.send.v2.feeselector.component.speed.ui.FeeSpeedSelectorContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class FeeSpeedSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: FeeSelectorComponentParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: FeeSpeedSelectorModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        if (state is FeeSelectorUM.Content) {
            FeeSpeedSelectorContent(
                state = state as FeeSelectorUM.Content,
                intents = model,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            appComponentContext: AppComponentContext,
            params: FeeSelectorComponentParams,
        ): FeeSpeedSelectorComponent
    }
}