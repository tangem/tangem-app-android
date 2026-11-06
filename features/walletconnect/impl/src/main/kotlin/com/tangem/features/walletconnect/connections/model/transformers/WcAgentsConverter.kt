package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.mcp.repository.McpSessionsLce
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.walletconnect.connections.entity.McpAgentInfo
import com.tangem.features.walletconnect.connections.entity.WcAgentsUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

/**
 * Builds the Connected Agents section of one wallet.
 *
 * The agent list is read for every wallet at once, so loading and failure are shared — only the
 * agents themselves are picked out per wallet.
 */
internal object WcAgentsConverter : Converter<WcAgentsConverter.Input, WcAgentsUM> {

    override fun convert(value: Input): WcAgentsUM = value.sessions.fold(
        ifLoading = { WcAgentsUM.Loading },
        ifError = { WcAgentsUM.Error(isReloading = value.isReloading, onReloadClick = value.onReloadClick) },
        ifContent = { sessions ->
            WcAgentsUM.Content(
                agents = sessions
                    .filter { it.walletId == value.wallet.walletId }
                    .map { session -> value.createAgent(session) }
                    .toPersistentList(),
            )
        },
    )

    private fun Input.createAgent(session: McpSession) = McpAgentInfo(
        id = session.id,
        name = session.clientName ?: session.clientOrigin ?: session.clientId,
        // clientOrigin is the only verified part of a client's identity
        subtitle = session.clientOrigin.orEmpty(),
        iconUrl = session.clientIconUrl,
        onClick = { onAgentClick(session, wallet.name) },
    )

    data class Input(
        val wallet: UserWallet,
        val sessions: McpSessionsLce,
        val isReloading: Boolean,
        val onReloadClick: () -> Unit,
        val onAgentClick: (McpSession, String) -> Unit,
    )
}