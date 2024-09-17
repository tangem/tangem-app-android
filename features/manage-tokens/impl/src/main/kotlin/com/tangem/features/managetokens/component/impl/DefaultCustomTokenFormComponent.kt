package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.model.CustomTokenFormModel
import com.tangem.features.managetokens.ui.CustomTokenFormContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCustomTokenFormComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: CustomTokenFormComponent.Params,
) : CustomTokenFormComponent, AppComponentContext by context {

    private val model: CustomTokenFormModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        CustomTokenFormContent(
            modifier = modifier,
            model = state,
        )
    }

    @AssistedFactory
    interface Factory : CustomTokenFormComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CustomTokenFormComponent.Params,
        ): DefaultCustomTokenFormComponent
    }
}