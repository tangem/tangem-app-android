package com.tangem.features.mcp.impl.intent.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.mcp.impl.intent.McpIntentComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

@ModelScoped
internal class McpIntentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<McpIntentComponent.Params>()

    init {
        val request = params.request
        TangemLogger.i("MCP intent received: id=${request.id}, source=${request.source}")
    }

    fun onClose() = router.pop()
}