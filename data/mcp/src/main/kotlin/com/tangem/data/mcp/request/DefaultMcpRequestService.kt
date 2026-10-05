package com.tangem.data.mcp.request

import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

internal class DefaultMcpRequestService @Inject constructor() : McpRequestService {

    private val requests: Channel<McpRequest> = Channel(Channel.BUFFERED)

    override val requestFlow: Flow<McpRequest> = requests.receiveAsFlow()

    override fun request(request: McpRequest) {
        requests.trySend(request)
    }
}