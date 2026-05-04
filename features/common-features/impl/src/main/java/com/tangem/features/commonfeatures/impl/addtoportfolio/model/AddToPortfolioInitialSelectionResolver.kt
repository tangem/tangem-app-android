package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import arrow.core.getOrElse
import com.tangem.domain.markets.GetTokenMarketCryptoCurrency
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddAccount
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddWallet
import javax.inject.Inject

internal class AddToPortfolioInitialSelectionResolver @Inject constructor(
    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
) {

    suspend fun resolve(
        availableToAddData: AvailableToAddData,
        orderedNetworks: List<TokenMarketInfo.Network>,
        selectedWallet: UserWallet?,
        tokenParams: RawMarketToken,
        accountToAdd: AvailableToAddAccount? = null,
        preferredNetwork: TokenMarketInfo.Network? = null,
    ): InitialSelection? {
        if (availableToAddData.availableToAddWallets.isEmpty()) return null
        val fallbackNetwork = orderedNetworks.firstOrNull() ?: return null

        val walletOrder = orderedWallets(availableToAddData, selectedWallet)

        if (accountToAdd != null) {
            val ownerWallet = walletOrder.firstOrNull { entry ->
                entry.availableToAddAccounts.values.any { it === accountToAdd }
            } ?: walletOrder.first()
            val network = pickNetworkForExplicitAccount(
                userWallet = ownerWallet.userWallet,
                account = accountToAdd,
                orderedNetworks = orderedNetworks,
                tokenParams = tokenParams,
                preferredNetwork = preferredNetwork,
            )
            return InitialSelection(userWallet = ownerWallet.userWallet, account = accountToAdd, network = network)
        }

        for (walletEntry in walletOrder) {
            val account = pickAvailableAccount(walletEntry) ?: continue
            val network = pickAddableNetwork(
                userWallet = walletEntry.userWallet,
                account = account,
                orderedNetworks = orderedNetworks,
                tokenParams = tokenParams,
            ) ?: continue
            return InitialSelection(walletEntry.userWallet, account, network)
        }

        val fallbackWallet = walletOrder.first()
        val fallbackAccount = pickFallbackAccount(fallbackWallet) ?: return null
        return InitialSelection(fallbackWallet.userWallet, fallbackAccount, fallbackNetwork)
    }

    private fun orderedWallets(data: AvailableToAddData, selectedWallet: UserWallet?): List<AvailableToAddWallet> {
        val preferred = selectedWallet?.walletId?.let { data.availableToAddWallets[it] }
        return buildList {
            preferred?.let(::add)
            data.availableToAddWallets.values.forEach { entry ->
                if (entry !== preferred) add(entry)
            }
        }
    }

    private fun pickAvailableAccount(walletEntry: AvailableToAddWallet): AvailableToAddAccount? {
        val mainId = AccountId.forMainCryptoPortfolio(walletEntry.userWallet.walletId)
        return walletEntry.availableToAddAccounts[mainId]?.takeIf { it.isAvailableToAdd }
            ?: walletEntry.availableToAddAccounts.values.firstOrNull { it.isAvailableToAdd }
    }

    private fun pickFallbackAccount(walletEntry: AvailableToAddWallet): AvailableToAddAccount? {
        val mainId = AccountId.forMainCryptoPortfolio(walletEntry.userWallet.walletId)
        return walletEntry.availableToAddAccounts[mainId]
            ?: walletEntry.availableToAddAccounts.values.firstOrNull()
    }

    private suspend fun pickNetworkForExplicitAccount(
        userWallet: UserWallet,
        account: AvailableToAddAccount,
        orderedNetworks: List<TokenMarketInfo.Network>,
        tokenParams: RawMarketToken,
        preferredNetwork: TokenMarketInfo.Network?,
    ): TokenMarketInfo.Network {
        if (preferredNetwork != null) {
            val candidate = account.availableToAddNetworks
                .firstOrNull { it.networkId == preferredNetwork.networkId }
            if (
                candidate != null && hasDerivationFor(
                    userWallet = userWallet,
                    account = account,
                    network = candidate,
                    tokenParams = tokenParams,
                )
            ) {
                return candidate
            }
        }
        return pickAddableNetwork(
            userWallet = userWallet,
            account = account,
            orderedNetworks = orderedNetworks,
            tokenParams = tokenParams,
        ) ?: orderedNetworks.first()
    }

    private suspend fun pickAddableNetwork(
        userWallet: UserWallet,
        account: AvailableToAddAccount,
        orderedNetworks: List<TokenMarketInfo.Network>,
        tokenParams: RawMarketToken,
    ): TokenMarketInfo.Network? {
        val availableOrdered = orderedNetworks.filter { candidate ->
            account.availableToAddNetworks.any { it.networkId == candidate.networkId }
        }
        if (availableOrdered.isEmpty()) return null

        val withDerivation = availableOrdered.firstOrNull { network ->
            hasDerivationFor(userWallet = userWallet, account = account, network = network, tokenParams = tokenParams)
        }
        return withDerivation ?: availableOrdered.first()
    }

    private suspend fun hasDerivationFor(
        userWallet: UserWallet,
        account: AvailableToAddAccount,
        network: TokenMarketInfo.Network,
        tokenParams: RawMarketToken,
    ): Boolean {
        val derivationIndex = account.account.account.derivationIndex
        val currency = getTokenMarketCryptoCurrency(
            userWalletId = userWallet.walletId,
            tokenMarketParams = tokenParams,
            network = network,
            accountIndex = derivationIndex,
        ) ?: return false
        return networkHasDerivationUseCase(userWallet, currency.network).getOrElse { false }
    }

    data class InitialSelection(
        val userWallet: UserWallet,
        val account: AvailableToAddAccount,
        val network: TokenMarketInfo.Network,
    )
}