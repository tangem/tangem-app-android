package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.tangem.domain.tokens.repository.NetworksCompatibilityRepository
import com.tangem.domain.wallets.models.UserWalletId

class CheckCurrencyCompatibilityUseCase(private val repository: NetworksCompatibilityRepository) {

    suspend operator fun invoke(
        networkId: String,
        isMainNetwork: Boolean,
        userWalletId: UserWalletId,
    ): Either<CurrencyCompatibilityError, Unit> {
        return either {
            catch(
                block = {
                    val error = getErrorIfNotSupported(networkId, isMainNetwork, userWalletId)
                    error?.left() ?: Unit.right()
                },
                catch = { raise(CurrencyCompatibilityError.UnsupportedBlockchain) },
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun getErrorIfNotSupported(
        networkId: String,
        isMainNetwork: Boolean,
        userWalletId: UserWalletId,
    ): CurrencyCompatibilityError? {
        if (isMainNetwork) {
            val supported = repository.isNetworkSupported(networkId, userWalletId)
            if (!supported) return CurrencyCompatibilityError.UnsupportedBlockchain
        } else {
            val solanaTokensSupportedOrNotSolanaNetwork =
                repository.areSolanaTokensSupportedIfRelevant(networkId, userWalletId)
            if (!solanaTokensSupportedOrNotSolanaNetwork) return CurrencyCompatibilityError.SolanaTokensUnsupported
            val tokensSupported = repository.areTokensSupportedByNetwork(networkId, userWalletId)
            if (!tokensSupported) return CurrencyCompatibilityError.UnsupportedCurve
        }
        return null
    }
}

sealed class CurrencyCompatibilityError {
    object SolanaTokensUnsupported : CurrencyCompatibilityError()
    object UnsupportedCurve : CurrencyCompatibilityError()
    object UnsupportedBlockchain : CurrencyCompatibilityError()
}