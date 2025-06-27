package com.tangem.features.send.v2.feeselector

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.feeselector.model.FeeSelectorModel
import com.tangem.features.send.v2.feeselector.ui.FeeSelectorModalBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultFeeSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: FeeSelectorComponent.Params,
) : FeeSelectorComponent, AppComponentContext by appComponentContext {

    private val model: FeeSelectorModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        FeeSelectorModalBottomSheet(onDismiss = ::dismiss, state = TODO())
    }

    // Temporary workaround, to use test this component
    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = router::pop)
        FeeSelectorModalBottomSheet(onDismiss = ::dismiss, state = state)
    }

    @AssistedFactory
    interface Factory : FeeSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: FeeSelectorComponent.Params,
        ): DefaultFeeSelectorComponent
    }
}