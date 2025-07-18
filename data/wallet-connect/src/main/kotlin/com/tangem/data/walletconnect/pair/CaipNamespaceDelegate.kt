package com.tangem.data.walletconnect.pair

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.models.network.Network
import com.tangem.data.walletconnect.model.CAIP10
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

internal class CaipNamespaceDelegate(
    private val namespaceConverters: Set<WcNamespaceConverter>,
    private val walletManagersFacade: WalletManagersFacade,
    private val wcNetworksConverter: WcNetworksConverter,
) {

    suspend fun associate(
        sessionProposal: Wallet.Model.SessionProposal,
        sessionForApprove: WcSessionApprove,
    ): Map<String, Wallet.Model.Namespace.Session> {
        val userWallet = sessionForApprove.wallet
        val result = mutableMapOf<String, Session>()

        wcNetworksConverter.convertNetworksForApprove(sessionForApprove)
            .mapNotNull { createCAIP10(userWallet.walletId, it) }
            .forEach { account ->
                val namespaceKey = account.chainId.namespace
                val session = result.getOrPut(namespaceKey) { Session() }
                val requiredNamespaces = sessionProposal.requiredNamespaces
                val optionalNamespaces = sessionProposal.optionalNamespaces
                val methods = buildSet {
                    requiredNamespaces[namespaceKey]?.methods?.let { addAll(it) }
                    optionalNamespaces[namespaceKey]?.methods?.let { addAll(it) }
                }
                val events = buildSet {
                    requiredNamespaces[namespaceKey]?.events?.let { addAll(it) }
                    optionalNamespaces[namespaceKey]?.events?.let { addAll(it) }
                }
                session.chains.add(account.chainId.raw)
                session.accounts.add(account.raw)
                session.methods.addAll(methods)
                session.events.addAll(events)
            }
        return result.mapValues { (_, session) ->
            Wallet.Model.Namespace.Session(
                chains = session.chains.toList(),
                accounts = session.accounts.toList(),
                methods = session.methods.toList(),
                events = session.events.toList(),
            )
        }
    }

    private suspend fun createCAIP10(userWalletId: UserWalletId, network: Network): CAIP10? {
        val address = walletManagersFacade.getDefaultAddress(userWalletId, network)
        val chainId = namespaceConverters.firstNotNullOfOrNull { it.toCAIP2(network) }
        if (chainId == null || address == null) return null
        return CAIP10(chainId = chainId, accountAddress = address)
    }

    private data class Session(
        val chains: MutableSet<String> = mutableSetOf(),
        val accounts: MutableSet<String> = mutableSetOf(),
        val methods: MutableSet<String> = mutableSetOf(),
        val events: MutableSet<String> = mutableSetOf(),
    )
}