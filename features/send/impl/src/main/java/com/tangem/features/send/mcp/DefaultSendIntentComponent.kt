package com.tangem.features.send.mcp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.api.mcp.SendIntentComponent
import com.tangem.features.send.mcp.model.SendIntentModel
import com.tangem.features.send.mcp.ui.SendIntentModal
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendIntentComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: SendIntentComponent.Params,
) : SendIntentComponent, AppComponentContext by appComponentContext {

    private val model: SendIntentModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val offer by model.offer.collectAsStateWithLifecycle()
        offer?.let { SendIntentModal(state = it, onDismiss = model::onClose) }
    }

    @AssistedFactory
    interface Factory : SendIntentComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendIntentComponent.Params,
        ): DefaultSendIntentComponent
    }
}