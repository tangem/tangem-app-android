package com.tangem.data.walletconnect.pair

import com.reown.walletkit.client.Wallet
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcSessionProposal.ProposalNetwork
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase

internal class AssociateNetworksDelegate constructor(
    private val namespaceConverters: Map<NamespaceKey, WcNamespaceConverter>,
    private val getWallets: GetWalletsUseCase,
    private val currenciesRepository: CurrenciesRepository,
    private val excludedBlockchains: ExcludedBlockchains,
) {

    @Throws(WcPairError.UnsupportedNetworks::class)
    suspend fun associate(sessionProposal: Wallet.Model.SessionProposal): Map<UserWallet, ProposalNetwork> {
        val userWallets = getWallets.invokeSync().filter { it.isMultiCurrency }
        val requiredNamespaces: Set<CAIP2> = sessionProposal.requiredNamespaces.setOfChainId()
        val optionalNamespaces: Set<CAIP2> = sessionProposal.optionalNamespaces.setOfChainId()

        return userWallets
            .associateWith { wallet -> mapNetworksForWallet(wallet, requiredNamespaces, optionalNamespaces) }
    }

    private suspend fun mapNetworksForWallet(
        wallet: UserWallet,
        requiredNamespaces: Set<CAIP2>,
        optionalNamespaces: Set<CAIP2>,
    ): ProposalNetwork {
        val walletNetworks = currenciesRepository.getMultiCurrencyWalletCurrenciesSync(wallet.walletId)
            .filterIsInstance<CryptoCurrency.Coin>()
            .map { it.network }

        val unknownRequired = mutableSetOf<String>()
        val missingRequired = mutableSetOf<Network>()
        val required = mutableSetOf<Network>()
        val available = mutableSetOf<Network>()
        val notAdded = mutableSetOf<Network>()

        fun CAIP2.toBlockchain() = namespaceConverters[NamespaceKey(this.namespace)]?.toBlockchain(this)
        fun Blockchain.toNetwork() = getNetwork(
            blockchain = this,
            extraDerivationPath = null,
            scanResponse = wallet.scanResponse,
            excludedBlockchains = excludedBlockchains,
        )

        requiredNamespaces.forEach { chainId ->
            val blockchain = chainId.toBlockchain()
            if (blockchain == null) {
                unknownRequired.add(missingNetworkName(chainId))
                return@forEach
            }
            val wcNetwork = blockchain.toNetwork()
            if (wcNetwork == null) {
                unknownRequired.add(missingNetworkName(blockchain))
                return@forEach
            }
            val walletNetwork = walletNetworks.find { network -> wcNetwork.id == network.id }
            if (walletNetwork == null) {
                missingRequired.add(wcNetwork)
            } else {
                required.add(walletNetwork)
            }
        }
        optionalNamespaces.forEach { chainId ->
            val wcNetwork = chainId.toBlockchain()?.toNetwork() ?: return@forEach
            val walletNetwork = walletNetworks.find { network -> wcNetwork.id == network.id }
            if (walletNetwork == null) {
                available.add(wcNetwork)
            } else {
                notAdded.add(walletNetwork)
            }
        }
        if (unknownRequired.isNotEmpty()) throw WcPairError.UnsupportedNetworks(unknownRequired)
        return ProposalNetwork(
            wallet = wallet,
            missingRequired = missingRequired,
            required = required,
            available = available,
            notAdded = notAdded,
        )
    }

    private fun Map<String, Wallet.Model.Namespace.Proposal>.setOfChainId(): Set<CAIP2> =
        this.values.flatMap { proposal -> proposal.chains ?: listOf() }
            .mapNotNull { rawChainId -> CAIP2.fromRaw(rawChainId) }.toSet()

    private fun missingNetworkName(chainId: CAIP2): String = chainId.namespace.replaceFirstChar(Char::titlecase)
    private fun missingNetworkName(blockchain: Blockchain): String = blockchain.getCoinName()
}