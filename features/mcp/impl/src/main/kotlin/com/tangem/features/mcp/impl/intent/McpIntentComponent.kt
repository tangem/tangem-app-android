package com.tangem.features.mcp.impl.intent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.impl.intent.model.McpIntentModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Has no UI yet: it only frees the request queue slot once shown. */
internal class McpIntentComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: McpIntentModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        LaunchedEffect(Unit) { model.onClose() }
    }

    internal data class Params(val request: McpRequest.Intent)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): McpIntentComponent
    }
}