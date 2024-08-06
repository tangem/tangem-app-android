package com.tangem.features.disclaimer.impl.component.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.disclaimer.api.components.DisclaimerComponent
import com.tangem.features.disclaimer.impl.model.DisclaimerModel
import com.tangem.features.disclaimer.impl.ui.DisclaimerScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultDisclaimerComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: DisclaimerComponent.Params,
) : DisclaimerComponent, AppComponentContext by context {

    private val model: DisclaimerModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        BackHandler(onBack = state.popBack)
        DisclaimerScreen(state = state)
    }

    @AssistedFactory
    interface Factory : DisclaimerComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: DisclaimerComponent.Params,
        ): DefaultDisclaimerComponent
    }
}