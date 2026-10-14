package com.tangem.data.mcp.converter

import com.tangem.data.mcp.api.models.response.McpClientResponse
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class McpClientConverter @Inject constructor() : Converter<McpClientResponse, McpAgent> {

    override fun convert(value: McpClientResponse): McpAgent = McpAgent(
        clientId = value.clientId,
        origin = value.clientOrigin,
        name = value.clientName,
        iconUrl = value.clientIconUrl,
    )
}