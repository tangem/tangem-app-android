package com.tangem.data.walletconnect.pair

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcSessionProposal.ProposalNetwork
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase

internal class AssociateNetworksDelegate constructor(
    private val namespaceConverters: Set<WcNamespaceConverter>,
    private val getWallets: GetWalletsUseCase,
    private val currenciesRepository: CurrenciesRepository,
) {

    @Throws(WcPairError.UnsupportedNetworks::class)
    suspend fun associate(sessionProposal: Wallet.Model.SessionProposal): Map<UserWallet, ProposalNetwork> {
        val userWallets = getWallets.invokeSync().filter { it.isMultiCurrency }
        val requiredNamespaces: Set<String> = sessionProposal.requiredNamespaces.setOfChainId()
        val optionalNamespaces: Set<String> = sessionProposal.optionalNamespaces.setOfChainId()

        return userWallets
            .associateWith { wallet -> mapNetworksForWallet(wallet, requiredNamespaces, optionalNamespaces) }
    }

    private suspend fun mapNetworksForWallet(
        wallet: UserWallet,
        requiredNamespaces: Set<String>,
        optionalNamespaces: Set<String>,
    ): ProposalNetwork {
        val walletNetworks = currenciesRepository.getMultiCurrencyWalletCurrenciesSync(wallet.walletId)
            .filterIsInstance<CryptoCurrency.Coin>()
            .map { it.network }

        val unknownRequired = mutableSetOf<String>()
        val missingRequired = mutableSetOf<Network>()
        val required = mutableSetOf<Network>()
        val available = mutableSetOf<Network>()
        val notAdded = mutableSetOf<Network>()

        requiredNamespaces.forEach { chainId ->
            val wcNetwork = namespaceConverters.firstNotNullOfOrNull { it.toNetwork(chainId, wallet) }
            if (wcNetwork == null) {
                unknownRequired.add(missingNetworkName(chainId))
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
            val wcNetwork = namespaceConverters.firstNotNullOfOrNull { it.toNetwork(chainId, wallet) }
                ?: return@forEach
            val walletNetwork = walletNetworks.find { network -> wcNetwork.id == network.id }
            if (walletNetwork != null) {
                available.add(walletNetwork)
            } else {
                notAdded.add(wcNetwork)
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

    private fun Map<String, Wallet.Model.Namespace.Proposal>.setOfChainId(): Set<String> =
        this.values.flatMap { proposal -> proposal.chains ?: listOf() }.toSet()

    private fun missingNetworkName(chainId: String): String = chainId.replaceFirstChar(Char::titlecase)
}