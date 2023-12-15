package com.tangem.managetokens.presentation.managetokens.viewmodels

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.extensions.supportedTokens
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet

class TokenCompatibility(private val selectedWallet: UserWallet) {

    fun check(blockchain: Blockchain, isMainNetwork: Boolean): Either<AddTokenError, Unit> {
        val error = if (isMainNetwork) isUnsupportedBlockchain(blockchain) else isUnsupportedToken(blockchain)
        return error?.left() ?: Unit.right()
    }

    private fun isUnsupportedToken(blockchain: Blockchain): AddTokenError? {
        val cardTypesResolver = selectedWallet.scanResponse.cardTypesResolver
        val blockchainsSupportingTokens = selectedWallet.scanResponse.card.supportedTokens(cardTypesResolver)

        if (blockchain == Blockchain.Solana && !blockchainsSupportingTokens.contains(Blockchain.Solana)) {
            return AddTokenError.SolanaTokensUnsupported
        }
        val canHandleToken = selectedWallet.scanResponse.card.canHandleToken(
            supportedTokens = blockchainsSupportingTokens,
            blockchain = blockchain,
            cardTypesResolver = cardTypesResolver,
        )
        if (!canHandleToken) {
            return AddTokenError.UnsupportedCurve
        }
        return null
    }

    private fun isUnsupportedBlockchain(blockchain: Blockchain): AddTokenError? {
        val isSupported = selectedWallet.scanResponse.card.canHandleBlockchain(
            blockchain = blockchain,
            cardTypesResolver = selectedWallet.scanResponse.cardTypesResolver,
        )
        return if (!isSupported) AddTokenError.UnsupportedBlockchain else null
    }

    sealed class AddTokenError {
        object SolanaTokensUnsupported : AddTokenError()
        object UnsupportedCurve : AddTokenError()
        object UnsupportedBlockchain : AddTokenError()
    }
}