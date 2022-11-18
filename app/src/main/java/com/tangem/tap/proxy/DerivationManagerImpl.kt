package com.tangem.tap.proxy

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.NonNativeToken
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.tokens.redux.TokensMiddleware
import kotlin.coroutines.suspendCoroutine

class DerivationManagerImpl(
    private val tokesMiddleware: TokensMiddleware,
    private val appStateHolder: AppStateHolder,
) : DerivationManager {

    override suspend fun deriveMissingBlockchains(currency: Currency) = suspendCoroutine<Boolean> { continuation ->
        val blockchain = Blockchain.fromNetworkId(currency.networkId)
        val card = appStateHolder.getActualCard()
        if (blockchain != null && card != null) {
            val appToken = if (currency is NonNativeToken) {
                Token(
                    symbol = currency.symbol,
                    contractAddress = currency.contractAddress,
                    decimals = currency.decimalCount,
                )
            } else null
            val blockchainNetwork = BlockchainNetwork(blockchain, card)
            val appCurrency = com.tangem.tap.features.wallet.models.Currency.fromBlockchainNetwork(
                blockchainNetwork,
                appToken,
            )
            if (appStateHolder.scanResponse != null) {
                tokesMiddleware.deriveMissingBlockchains(
                    scanResponse = appStateHolder.scanResponse,
                    listOf(appCurrency),
                ) {
                    continuation.resumeWith(Result.success(true))
                }
            }
        } else {
            continuation.resumeWith(Result.failure(IllegalStateException("no blockchain or card found")))
        }
    }

    override fun hasDerivation(networkId: String): Boolean {
        val scanResponse = appStateHolder.scanResponse
        val blockchain = Blockchain.fromNetworkId(networkId)
        if (scanResponse != null && blockchain != null) {
            val derivationPath = blockchain.derivationPath(appStateHolder.getActualCard()?.derivationStyle)?.rawPath
            if (derivationPath.isNullOrEmpty()) return false
            return scanResponse.hasDerivation(
                blockchain,
                derivationPath,
            )
        }
        return false
    }
}