package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
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
 * are not readable, so there would be nothing behind its tab. Agents that are still loading don't
 * put a wallet in the row either: the row would collapse again once the list arrives, and with
 * nothing else to show the screen is a skeleton anyway.
 *
 * A failed agent read is the one case where a wallet with nothing else is still listed — [currentWalletId]
 * alone — so the owner is given the reload instead of being told there are no connections.
 */
@Suppress("LongParameterList")
internal class WcConnectionsTransformer(
    private val wallets: List<UserWallet>,
    private val sessionsMap: Map<UserWallet, List<WcSession>>,
    private val accountLists: List<AccountList>,
    private val mcpSessions: Lce<Throwable, List<McpSession>>,
    private val isAgentsReloading: Boolean,
    private val isAccountMode: Boolean,
    private val currentWalletId: UserWalletId,
    private val selectedWalletId: String?,
    private val onWalletSelect: (String) -> Unit,
    private val onAgentsReloadClick: () -> Unit,
    private val openAppInfoModal: (WcSession) -> Unit,
    private val openMcpSessionModal: (McpSession, String) -> Unit,
) : Transformer<WcConnectionsState> {

    override fun transform(prevState: WcConnectionsState): WcConnectionsState {
        val unlockedWallets = wallets.filterNot { it.isLocked }
        val walletsWithConnections = unlockedWallets.mapNotNull(::createWallet).toPersistentList()

        val connections = when {
            walletsWithConnections.isNotEmpty() -> createContent(walletsWithConnections)
            mcpSessions.isLoading() -> WcConnections.Loading
            mcpSessions.isError() -> {
                val currentWallet = unlockedWallets.firstOrNull { it.walletId == currentWalletId }

                currentWallet
                    ?.let { createAgentsOnlyWallet(it) }
                    ?.let { wallet -> createContent(persistentListOf(wallet)) }
                    ?: WcConnections.Empty
            }
            else -> WcConnections.Empty
        }

        return prevState.copy(connections = connections)
    }

    private fun createContent(walletsWithConnections: ImmutableList<WcWalletConnectionsUM>) = WcConnections.Content(
        wallets = walletsWithConnections,
        // The selected wallet may have just lost its last connection and left the row.
        selectedWalletId = walletsWithConnections
            .firstOrNull { it.userWalletId == selectedWalletId }
            ?.userWalletId
            ?: walletsWithConnections.first().userWalletId,
        onWalletSelect = onWalletSelect,
    )

    private fun createWallet(wallet: UserWallet): WcWalletConnectionsUM? {
        val agents = createAgents(wallet)
        val dAppGroups = createDAppGroups(wallet)
        val hasAgents = agents is WcAgentsUM.Content && agents.agents.isNotEmpty()

        if (!hasAgents && dAppGroups.isEmpty()) return null

        return WcWalletConnectionsUM(
            userWalletId = wallet.walletId.stringValue,
            walletName = wallet.name,
            agents = agents,
            dAppGroups = dAppGroups,
        )
    }

    private fun createAgentsOnlyWallet(wallet: UserWallet) = WcWalletConnectionsUM(
        userWalletId = wallet.walletId.stringValue,
        walletName = wallet.name,
        agents = createAgents(wallet),
        dAppGroups = persistentListOf(),
    )

    private fun createAgents(wallet: UserWallet): WcAgentsUM = mcpSessions.fold(
        ifLoading = { WcAgentsUM.Loading },
        ifError = { WcAgentsUM.Error(isReloading = isAgentsReloading, onReloadClick = onAgentsReloadClick) },
        ifContent = { sessions ->
            WcAgentsUM.Content(
                agents = sessions
                    .filter { it.walletId == wallet.walletId }
                    .map { session -> createAgent(session = session, walletName = wallet.name) }
                    .toPersistentList(),
            )
        },
    )

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