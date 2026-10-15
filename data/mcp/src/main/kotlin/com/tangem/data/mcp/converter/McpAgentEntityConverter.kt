package com.tangem.data.mcp.converter

import com.tangem.data.mcp.store.McpAgentEntity
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.utils.converter.TwoWayConverter
import javax.inject.Inject

internal class McpAgentEntityConverter @Inject constructor() : TwoWayConverter<McpAgent, McpAgentEntity> {

    override fun convert(value: McpAgent): McpAgentEntity = McpAgentEntity(
        clientId = value.clientId,
        origin = value.origin,
        name = value.name,
        iconUrl = value.iconUrl,
    )

    override fun convertBack(value: McpAgentEntity): McpAgent = McpAgent(
        clientId = value.clientId,
        origin = value.origin,
        name = value.name,
        iconUrl = value.iconUrl,
    )
}