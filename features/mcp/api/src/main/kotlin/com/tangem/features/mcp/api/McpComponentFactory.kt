package com.tangem.features.mcp.api

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.mcp.models.McpRequest

interface McpComponentFactory {

    fun create(context: AppComponentContext, request: McpRequest): ComposableContentComponent
}