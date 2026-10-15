package com.tangem.data.mcp.store

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.core.remote.json.NetworkJson
import com.tangem.data.mcp.converter.McpAgentEntityConverter
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getJsonOrNull
import com.tangem.datasource.local.preferences.utils.getJsonSyncOrNull
import com.tangem.datasource.local.preferences.utils.setJson
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val TAG = "MCP"

/**
 * Agents the authorization server has described, kept across app restarts by `clientId`.
 *
 * Reading and writing never throw: the store only saves a request to the server, so a storage failure
 * must not cost the owner the agent's description.
 */
internal class McpAgentStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    @NetworkJson private val json: Json,
    private val converter: McpAgentEntityConverter,
) {

    suspend fun get(clientId: String): McpAgent? {
        return runSuspendCatching { appPreferencesStore.getJsonSyncOrNull(MCP_AGENTS_KEY, json, SERIALIZER) }
            .onFailure { TangemLogger.withTag(TAG).e("Stored agents cannot be read", it) }
            .getOrNull()
            ?.get(clientId)
            ?.let(converter::convertBack)
    }

    suspend fun store(agent: McpAgent) {
        runSuspendCatching {
            appPreferencesStore.editData { preferences ->
                val agents = preferences.getJsonOrNull(MCP_AGENTS_KEY, json, SERIALIZER).orEmpty()

                preferences.setJson(
                    key = MCP_AGENTS_KEY,
                    json = json,
                    serializer = SERIALIZER,
                    value = agents + (agent.clientId to converter.convert(agent)),
                )
            }
        }
            .onFailure { TangemLogger.withTag(TAG).e("Agent ${agent.clientId} cannot be stored", it) }
    }

    private companion object {

        val MCP_AGENTS_KEY by lazy { stringPreferencesKey(name = "mcpAgents") }

        val SERIALIZER = MapSerializer(String.serializer(), McpAgentEntity.serializer())
    }
}