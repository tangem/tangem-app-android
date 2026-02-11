package com.tangem.data.walletconnect.pair

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.Wallet.Model.Namespace
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcSessionProposal.ProposalNetwork
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

internal class AssociateNetworksDelegate(
    private val networksConverter: WcNetworksConverter,
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val getWallets: GetWalletsUseCase,
) {

    @Throws(WcPairError.UnsupportedBlockchains::class)
    suspend fun associate(sessionProposal: Wallet.Model.SessionProposal): Map<UserWallet, ProposalNetwork> {
        val userWallets = getWallets.invokeSync().filter { it.isMultiCurrency }
        val requiredNamespaces: Set<String> = sessionProposal.requiredNamespaces.setOfChainId()
        val optionalNamespaces: Set<String> = sessionProposal.optionalNamespaces.setOfChainId()
            // remove duplicates
            .subtract(requiredNamespaces)

        return userWallets.associateWith { wallet ->
            mapNetworksForPortfolio(wallet, null, requiredNamespaces, optionalNamespaces, sessionProposal)
        }
    }

    @Throws(WcPairError.UnsupportedBlockchains::class)
    suspend fun associateAccounts(sessionProposal: Wallet.Model.SessionProposal): Map<AccountId, ProposalNetwork> {
        val userWallets = getWallets.invokeSync()
            .filter { it.isMultiCurrency && !it.isLocked }
        val requiredNamespaces: Set<String> = sessionProposal.requiredNamespaces.setOfChainId()
        val optionalNamespaces: Set<String> = sessionProposal.optionalNamespaces.setOfChainId()
            // remove duplicates
            .subtract(requiredNamespaces)
        val allAccounts = multiAccountListSupplier.invoke()
            .filter { it.isNotEmpty() }
            .first()
            .filter { accountList -> userWallets.any { accountList.userWalletId == it.walletId } }
            .map { accountList -> accountList to userWallets.first { it.walletId == accountList.userWalletId } }

        return allAccounts
            .map { (accountList, wallet) -> accountList.accounts.map { account -> account to wallet } }
            .flatten()
            .associate { (account, wallet) ->
                account.accountId to mapNetworksForPortfolio(
                    wallet = wallet,
                    account = account,
                    requiredNamespaces = requiredNamespaces,
                    optionalNamespaces = optionalNamespaces,
                    sessionProposal = sessionProposal,
                )
            }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun mapNetworksForPortfolio(
        wallet: UserWallet,
        account: Account?,
        requiredNamespaces: Set<String>,
        optionalNamespaces: Set<String>,
        sessionProposal: Wallet.Model.SessionProposal,
    ): ProposalNetwork {
        val portfolioNetworks = account?.let { getAccountNetworks(it.accountId) }
            ?: getWalletNetworks(userWalletId = wallet.walletId)

        val unknownRequired = mutableSetOf<String>()
        val unknownOptional = mutableSetOf<String>()
        val missingRequired = mutableSetOf<Network>()
        val required = mutableSetOf<Network>()
        val available = mutableSetOf<Network>()
        val notAdded = mutableSetOf<Network>()

        requiredNamespaces.forEach { chainId ->
            val wcNetwork = networksConverter.createNetwork(chainId, wallet)
            if (wcNetwork == null) {
                unknownRequired.add(missingNetworkName(chainId))
                return@forEach
            }
            val portfolioNetwork = portfolioNetworks.find { network -> wcNetwork.rawId == network.rawId }

            if (portfolioNetwork == null) {
                missingRequired.add(wcNetwork)
            } else {
                required.add(portfolioNetwork)
            }
        }
        optionalNamespaces.forEach { chainId ->
            val wcNetwork = networksConverter.createNetwork(chainId, wallet)
            if (wcNetwork == null) {
                unknownOptional.add(missingNetworkName(chainId))
                return@forEach
            }
            val portfolioNetwork = portfolioNetworks.find { network -> wcNetwork.rawId == network.rawId }
            if (portfolioNetwork != null) {
                available.add(portfolioNetwork)
            } else {
                notAdded.add(wcNetwork)
            }
        }
        if (unknownRequired.isNotEmpty()) {
            throw WcPairError.UnsupportedBlockchains(unknownRequired, sessionProposal.name)
        }
        val isUnknownOptional = unknownOptional.isNotEmpty() && required.isEmpty() &&
            available.isEmpty() && missingRequired.isEmpty() && notAdded.isEmpty()
        if (isUnknownOptional) {
            throw WcPairError.UnsupportedBlockchains(unknownOptional, sessionProposal.name)
        }
        return ProposalNetwork(
            wallet = wallet,
            missingRequired = missingRequired,
            required = required,
            available = available,
            notAdded = notAdded,
            account = account,
        )
    }

    private suspend fun getWalletNetworks(userWalletId: UserWalletId): List<Network> {
        return networksConverter.getWalletNetworks(userWalletId)
            // flatten all derivation
            .distinctBy { it.rawId }
    }

    private suspend fun getAccountNetworks(accountId: AccountId): List<Network> {
        return networksConverter.getAccountNetworks(accountId)
            // flatten all derivation
            .distinctBy { it.rawId }
    }

    private fun missingNetworkName(chainId: String): String = chainId.replaceFirstChar(Char::titlecase)

    companion object {
        internal fun Map<String, Namespace.Proposal>.setOfChainId(): Set<String> =
            this.values.flatMap { proposal -> proposal.chains ?: listOf() }.toSet()
    }
}