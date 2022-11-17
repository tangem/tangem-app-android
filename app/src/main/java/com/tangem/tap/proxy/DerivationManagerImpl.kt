package com.tangem.tap.proxy

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.lib.crypto.DerivationManager
import com.tangem.tap.features.tokens.redux.TokensMiddleware

class DerivationManagerImpl(
    val tokesMiddleware: TokensMiddleware,
    val cardStateHolder: CardStateHolder,
) : DerivationManager {

    override suspend fun deriveMissingBlockchains(networkId: String) {
        if (cardStateHolder.scanResponse != null) { //todo how to get Currency list from token
            tokesMiddleware.deriveMissingBlockchains(
                cardStateHolder.scanResponse, emptyList(),
            ) {

            }
        }
    }

    override fun hasDerivation(networkId: String): Boolean {
        val scanResponse = cardStateHolder.scanResponse
        val blockchain = Blockchain.fromNetworkId(networkId)
        if (scanResponse != null && blockchain != null) {
            val derivationPath = blockchain.derivationPath(cardStateHolder.getActualCard()?.derivationStyle)?.rawPath
            if (derivationPath.isNullOrEmpty()) return false
            return scanResponse.hasDerivation(
                blockchain,
                derivationPath,
            )
        }
        return false
    }
}