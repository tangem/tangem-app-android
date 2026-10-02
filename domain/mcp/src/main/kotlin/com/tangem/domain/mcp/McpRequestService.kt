package com.tangem.domain.mcp

import com.tangem.domain.mcp.models.McpRequest
import kotlinx.coroutines.flow.Flow

/**
 * Entry point for everything an agent asks the wallet to do: a deep link, a scanned QR or any other
 * source hands the request over here, and whoever owns the MCP UI collects [requestFlow].
 */
interface McpRequestService {

    val requestFlow: Flow<McpRequest>

    fun request(request: McpRequest)
}