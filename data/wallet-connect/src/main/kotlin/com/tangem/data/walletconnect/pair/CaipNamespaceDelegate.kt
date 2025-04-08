package com.tangem.data.walletconnect.pair

import com.reown.walletkit.client.Wallet
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.walletconnect.model.CAIP10
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet

internal class CaipNamespaceDelegate constructor(
    private val namespaceConverters: Map<NamespaceKey, WcNamespaceConverter>,
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend fun associate(
        sessionProposal: Wallet.Model.SessionProposal,
        userWallet: UserWallet,
        networks: List<Network>,
    ): Map<String, Wallet.Model.Namespace.Session> {
        val converters = namespaceConverters.values

        val result = mutableMapOf<String, Session>()

        networks.map { network ->
            val blockchain = Blockchain.fromId(network.id.value)
            val address = walletManagersFacade.getDefaultAddress(userWallet.walletId, network)
            val chainId = converters.firstOrNull { it.toCAIP2(blockchain) != null }?.toCAIP2(blockchain)
            requireNotNull(chainId)
            requireNotNull(address)
            CAIP10(chainId = chainId, accountAddress = address)
        }.forEach { account ->
            val namespaceKey = account.chainId.namespace
            val session = result.getOrPut(namespaceKey) { Session() }
            val requiredNamespaces = sessionProposal.requiredNamespaces
            val optionalNamespaces = sessionProposal.optionalNamespaces
            val methods = buildSet {
                requiredNamespaces[namespaceKey]?.methods?.let { addAll(it) }
                optionalNamespaces[namespaceKey]?.methods?.let { addAll(it) }
            }
            session.chains.add(account.chainId.raw)
            session.accounts.add(account.raw)
            session.methods.addAll(methods)
        }
        return result.mapValues { (namespaceKey, session) ->
            Wallet.Model.Namespace.Session(
                chains = session.chains.toList(),
                accounts = session.accounts.toList(),
                methods = session.methods.toList(),
                events = session.events.toList(),
            )
        }
    }

    private data class Session(
        val chains: MutableSet<String> = mutableSetOf(),
        val accounts: MutableSet<String> = mutableSetOf(),
        val methods: MutableSet<String> = mutableSetOf(),
        val events: MutableSet<String> = mutableSetOf(),
    )
}