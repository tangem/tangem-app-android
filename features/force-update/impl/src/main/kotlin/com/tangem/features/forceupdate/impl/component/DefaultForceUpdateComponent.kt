package com.tangem.features.forceupdate.impl.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.forceupdate.ForceUpdateComponent
import com.tangem.features.forceupdate.impl.model.ForceUpdateModel
import com.tangem.features.forceupdate.impl.ui.ForceUpdateContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultForceUpdateComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: ForceUpdateComponent.Params,
) : ForceUpdateComponent, AppComponentContext by context {

    private val model: ForceUpdateModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        ForceUpdateContent(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : ForceUpdateComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: ForceUpdateComponent.Params,
        ): DefaultForceUpdateComponent
    }
}