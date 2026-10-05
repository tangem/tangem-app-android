package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Lays the connections out per wallet, which is what the tab row is built from.
 *
 * The wallets come from [wallets] rather than from [sessionsMap]'s keys: the map only holds wallets
 * with dApp sessions, so a wallet whose only connection is an agent would be missing from the row.
 *
 * A wallet with neither agents nor dApps is left out entirely, and so is a locked one — its sessions
 * are not readable, so there would be nothing behind its tab.
 */
@Suppress("LongParameterList")
internal class WcConnectionsTransformer(
    private val wallets: List<UserWallet>,
    private val sessionsMap: Map<UserWallet, List<WcSession>>,
    private val accountLists: List<AccountList>,
    private val mcpSessions: List<McpSession>,
    private val isAccountMode: Boolean,
    private val selectedWalletId: String?,
    private val onWalletSelect: (String) -> Unit,
    private val openAppInfoModal: (WcSession) -> Unit,
    private val openMcpSessionModal: (McpSession, String) -> Unit,
) : Transformer<WcConnectionsState> {

    override fun transform(prevState: WcConnectionsState): WcConnectionsState {
        val walletsWithConnections = wallets
            .filterNot { it.isLocked }
            .mapNotNull(::createWallet)
            .toPersistentList()

        val connections = if (walletsWithConnections.isEmpty()) {
            WcConnections.Empty
        } else {
            WcConnections.Content(
                wallets = walletsWithConnections,
                // The selected wallet may have just lost its last connection and left the row.
                selectedWalletId = walletsWithConnections
                    .firstOrNull { it.userWalletId == selectedWalletId }
                    ?.userWalletId
                    ?: walletsWithConnections.first().userWalletId,
                onWalletSelect = onWalletSelect,
            )
        }

        return prevState.copy(connections = connections)
    }

    private fun createWallet(wallet: UserWallet): WcWalletConnectionsUM? {
        val agents = mcpSessions
            .filter { it.walletId == wallet.walletId }
            .map { session -> createAgent(session = session, walletName = wallet.name) }
            .toPersistentList()
        val dAppGroups = createDAppGroups(wallet)

        if (agents.isEmpty() && dAppGroups.isEmpty()) return null

        return WcWalletConnectionsUM(
            userWalletId = wallet.walletId.stringValue,
            walletName = wallet.name,
            agents = agents,
            dAppGroups = dAppGroups,
        )
    }

    private fun createAgent(session: McpSession, walletName: String) = McpAgentInfo(
        id = session.id,
        name = session.clientName ?: session.clientOrigin ?: session.clientId,
        // clientOrigin is the only verified part of a client's identity
        subtitle = session.clientOrigin.orEmpty(),
        iconUrl = session.clientIconUrl,
        onClick = { openMcpSessionModal(session, walletName) },
    )

    private fun createDAppGroups(wallet: UserWallet): ImmutableList<WcDAppGroupUM> {
        val sessions = sessionsMap[wallet].orEmpty()
        if (sessions.isEmpty()) return persistentListOf()

        return if (isAccountMode) createAccountGroups(wallet, sessions) else createWalletGroup(wallet, sessions)
    }

    private fun createWalletGroup(wallet: UserWallet, sessions: List<WcSession>) = persistentListOf(
        WcDAppGroupUM(
            id = wallet.walletId.stringValue,
            title = null,
            connectedApps = sessions.map(::createAppInfo).toPersistentList(),
        ),
    )

    private fun createAccountGroups(wallet: UserWallet, sessions: List<WcSession>): ImmutableList<WcDAppGroupUM> {
        val accounts = accountLists.firstOrNull { it.userWalletId == wallet.walletId }
            ?.accounts
            ?.filterIsInstance<Account.CryptoPortfolio>()
            .orEmpty()

        return accounts.mapNotNull { account ->
            val accountSessions = sessions.filter { it.account.accountId == account.accountId }
            if (accountSessions.isEmpty()) return@mapNotNull null

            WcDAppGroupUM(
                id = wallet.walletId.stringValue + account.accountId.value,
                title = AccountTitleUM.Account(
                    prefixText = TextReference.EMPTY,
                    name = account.accountName.toUM().value,
                    icon = CryptoPortfolioIconConverter.convert(account.icon),
                ),
                connectedApps = accountSessions.map(::createAppInfo).toPersistentList(),
            )
        }.toPersistentList()
    }

    private fun createAppInfo(session: WcSession) = with(session.sdkModel) {
        WcConnectedAppInfo(
            name = appMetaData.name,
            iconUrl = appMetaData.icons.firstOrNull().orEmpty(),
            subtitle = WcAppSubtitleConverter.convert(appMetaData),
            verifiedState = WcDAppVerifiedStateConverter {}.convert(session.securityStatus to appMetaData.name),
            onClick = { openAppInfoModal(session) },
        )
    }
}