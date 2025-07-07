package com.tangem.data.walletconnect.pair

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.Wallet.Model.Namespace
import com.tangem.data.common.currency.isCustomCoin
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcSessionProposal.ProposalNetwork
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetWalletsUseCase

internal class AssociateNetworksDelegate(
    private val namespaceConverters: Set<WcNamespaceConverter>,
    private val getWallets: GetWalletsUseCase,
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend fun associate(wallet: UserWallet, namespaces: Map<String, Namespace.Session>): Set<Network> {
        val walletNetworks = getWalletNetworks(wallet)
        val namespacesSet = namespaces.values.flatMap { proposal -> proposal.chains ?: listOf() }.toSet()
        return namespacesSet.mapNotNullTo(mutableSetOf()) { chainId ->
            val wcNetwork = namespaceConverters
                .firstNotNullOfOrNull { it.toNetwork(chainId, wallet) } ?: return@mapNotNullTo null
            walletNetworks.find { network -> wcNetwork.id == network.id }
        }
    }

    @Throws(WcPairError.UnsupportedBlockchains::class)
    suspend fun associate(sessionProposal: Wallet.Model.SessionProposal): Map<UserWallet, ProposalNetwork> {
        val userWallets = getWallets.invokeSync().filter { it.isMultiCurrency }
        val requiredNamespaces: Set<String> = sessionProposal.requiredNamespaces.setOfChainId()
        val optionalNamespaces: Set<String> = sessionProposal.optionalNamespaces.setOfChainId()
            // remove duplicates
            .subtract(requiredNamespaces)

        return userWallets
            .associateWith { wallet -> mapNetworksForWallet(wallet, requiredNamespaces, optionalNamespaces) }
    }

    private suspend fun mapNetworksForWallet(
        wallet: UserWallet,
        requiredNamespaces: Set<String>,
        optionalNamespaces: Set<String>,
    ): ProposalNetwork {
        val walletNetworks = getWalletNetworks(wallet)

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

            if (walletNetwork == null || isCustomCoin(walletNetwork)) {
                missingRequired.add(wcNetwork)
            } else {
                required.add(walletNetwork)
            }
        }
        optionalNamespaces.forEach { chainId ->
            val wcNetwork = namespaceConverters.firstNotNullOfOrNull { it.toNetwork(chainId, wallet) }
                ?: return@forEach
            val walletNetwork = walletNetworks.find { network -> wcNetwork.id == network.id }
            if (walletNetwork != null && !isCustomCoin(walletNetwork)) {
                available.add(walletNetwork)
            } else {
                notAdded.add(wcNetwork)
            }
        }
        if (unknownRequired.isNotEmpty()) throw WcPairError.UnsupportedBlockchains(unknownRequired)
        return ProposalNetwork(
            wallet = wallet,
            missingRequired = missingRequired,
            required = required,
            available = available,
            notAdded = notAdded,
        )
    }

    private suspend fun getWalletNetworks(wallet: UserWallet): List<Network> =
        currenciesRepository.getMultiCurrencyWalletCurrenciesSync(wallet.walletId)
            .filterIsInstance<CryptoCurrency.Coin>()
            .map { it.network }

    private fun Map<String, Namespace.Proposal>.setOfChainId(): Set<String> =
        this.values.flatMap { proposal -> proposal.chains ?: listOf() }.toSet()

    private fun missingNetworkName(chainId: String): String = chainId.replaceFirstChar(Char::titlecase)
}