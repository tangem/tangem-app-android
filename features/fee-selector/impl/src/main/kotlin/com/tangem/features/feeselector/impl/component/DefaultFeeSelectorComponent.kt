package com.tangem.features.feeselector.impl.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.feeselector.api.component.FeeSelectorComponent
import com.tangem.features.feeselector.impl.model.FeeSelectorModel
import com.tangem.features.feeselector.impl.ui.FeeSelectorModalBottomSheet
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
        BackHandler(onBack = router::pop)
        FeeSelectorModalBottomSheet(onDismiss = ::dismiss, state = TODO())
    }

    @AssistedFactory
    interface Factory : FeeSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: FeeSelectorComponent.Params,
        ): DefaultFeeSelectorComponent
    }
}