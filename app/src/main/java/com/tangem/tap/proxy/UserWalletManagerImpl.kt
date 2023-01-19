package com.tangem.tap.proxy

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.Currency.NativeToken
import com.tangem.lib.crypto.models.Currency.NonNativeToken
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.ProxyFiatCurrency
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.WalletAction
import org.rekotlin.Action
import java.math.BigDecimal

class UserWalletManagerImpl(
    private val appStateHolder: AppStateHolder,
    private val walletManagerFactory: WalletManagerFactory,
) : UserWalletManager {

    override suspend fun getUserTokens(networkId: String): List<Currency> {
        val card = appStateHolder.getActualCard()
        val userTokensRepository =
            requireNotNull(appStateHolder.userTokensRepository) { "userTokensRepository is null" }
        return if (card != null) {
            userTokensRepository.getUserTokens(card)
                .filter { it.blockchain.toNetworkId() == networkId }
                .map {
                    if (it is com.tangem.tap.features.wallet.models.Currency.Token) {
                        NonNativeToken(
                            id = it.coinId ?: "",
                            name = it.currencyName,
                            symbol = it.currencySymbol,
                            networkId = it.blockchain.toNetworkId(),
                            contractAddress = it.token.contractAddress,
                            decimalCount = it.token.decimals,
                        )
                    } else {
                        NativeToken(
                            id = it.coinId ?: "",
                            name = it.currencyName,
                            symbol = it.currencySymbol,
                            networkId = it.blockchain.toNetworkId(),
                        )
                    }
                }
        } else {
            emptyList()
        }
    }

    override fun getWalletId(): String {
        return appStateHolder.getActualCard()?.let {
            UserWalletIdBuilder.card(it)
                .build()
                ?.stringValue
        }
            ?: ""
    }

    override suspend fun isTokenAdded(currency: Currency): Boolean {
        val card = appStateHolder.getActualCard()
        val userTokensRepository =
            requireNotNull(appStateHolder.userTokensRepository) { "userTokensRepository is null" }
        if (card != null) {
            return userTokensRepository.getUserTokens(card)
                .any { it.coinId.equals(currency.id) } // todo ensure that its the same ids
        }
        return false
    }

    // todo check is it sync operation
    override fun addToken(currency: Currency) {
        val action = if (currency is NativeToken) {
            val card = appStateHolder.getActualCard()
            if (card != null) addNativeTokenToWalletAction(currency, card) else error("Card not found")
        } else {
            addNonNativeTokenToWalletAction(currency as NonNativeToken)
        }
        val mainStore = requireNotNull(appStateHolder.mainStore) { "mainStore is null" }
        mainStore.dispatch(action)
    }

    override fun getWalletAddress(networkId: String): String {
        val blockchain = Blockchain.fromNetworkId(networkId)
        val card = appStateHolder.getActualCard()
        if (blockchain != null && card != null) {
            val blockchainNetwork = BlockchainNetwork(blockchain, card)
            val walletManager = appStateHolder.walletState?.getWalletManager(blockchainNetwork)
            if (walletManager != null) {
                return walletManager.wallet.address
            } else {
                error("no wallet manager found")
            }
        } else {
            error("no blockchain or card found")
        }
    }

    override fun getCurrentWalletTokensBalance(networkId: String): Map<String, ProxyAmount> {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain)
        return walletManager.wallet.amounts.map { entry ->
            val amount = entry.value
            amount.currencySymbol to ProxyAmount(
                amount.currencySymbol,
                amount.value ?: BigDecimal.ZERO,
                amount.decimals,
            )
        }.toMap()
    }

    override fun getUserAppCurrency(): ProxyFiatCurrency {
        val appCurrency = appStateHolder.appFiatCurrency
        return ProxyFiatCurrency(
            code = appCurrency.code,
            name = appCurrency.name,
            symbol = appCurrency.symbol,
        )
    }

    override fun getCurrencyByNetworkId(networkId: String): String {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        return blockchain.currency
    }

    private fun addNativeTokenToWalletAction(token: NativeToken, card: CardDTO): Action {
        val blockchain = Blockchain.fromNetworkId(token.networkId)
        val scanResponse = appStateHolder.scanResponse
        if (blockchain != null && scanResponse != null) {
            val blockchainNetwork = BlockchainNetwork(blockchain, card)
            var walletManager = appStateHolder.walletState?.getWalletManager(blockchainNetwork)
            if (walletManager == null) {
                walletManager = walletManagerFactory.makeWalletManagerForApp(
                    scanResponse = scanResponse,
                    blockchain = blockchain,
                    derivationParams = createDerivationParams(card.derivationStyle),
                )
            }
            return WalletAction.MultiWallet.AddBlockchain(
                blockchain = blockchainNetwork,
                walletManager = walletManager,
                save = true,
            )
        } else {
            error("blockchain is not supported")
        }
    }

    private fun addNonNativeTokenToWalletAction(token: NonNativeToken): Action {
        val card = appStateHolder.getActualCard()
        val blockchain = Blockchain.fromNetworkId(token.networkId)
        if (card != null && blockchain != null) {
            return WalletAction.MultiWallet.AddToken(
                token = com.tangem.blockchain.common.Token(
                    id = token.id,
                    name = token.name,
                    symbol = token.symbol,
                    contractAddress = token.contractAddress,
                    decimals = token.decimalCount,
                ),
                blockchain = BlockchainNetwork(
                    blockchain,
                    card,
                ),
                save = true,
            )
        } else {
            error("no card or blockchain found")
        }
    }

    private fun createDerivationParams(derivationStyle: DerivationStyle?): DerivationParams? {
        // todo clarify if its need to add Custom
        return derivationStyle?.let { DerivationParams.Default(derivationStyle) }
    }

    private fun getActualWalletManager(blockchain: Blockchain): WalletManager {
        val card = requireNotNull(appStateHolder.getActualCard()) { "card not found" }
        val blockchainNetwork = BlockchainNetwork(blockchain, card)
        return requireNotNull(appStateHolder.walletState?.getWalletManager(blockchainNetwork)) {
            "No wallet manager found"
        }
    }
}
