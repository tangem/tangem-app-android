package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketOnboardingError

/**
 * Obtains the CLOB credentials of the wallet's owner address and persists them against [UserWalletId]. Deriving is deterministic and returns
 * the existing key, so it is tried first; creating is attempted only when no key exists yet, which keeps a
 * repeated onboarding from producing a second, competing key set.
 */
class DeriveApiCredentialsUseCase(
    private val polymarketRepository: PolymarketRepository,
    private val credentialsStore: PolymarketCredentialsStore,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        ownerAddress: String,
        l1Signature: String,
        timestamp: String,
    ): Either<PolymarketOnboardingError, PolymarketApiCredentials> {
        credentialsStore.get(userWalletId = userWalletId)?.let { return it.right() }

        val headers = PolymarketL1Headers(
            address = ownerAddress,
            signature = l1Signature,
            timestamp = timestamp,
            nonce = CLOB_AUTH_NONCE,
        )

        val derived = polymarketRepository.deriveApiCredentials(headers = headers)
        val credentials = if (derived.leftOrNull() == PolymarketAuthError.KeyNotFound) {
            polymarketRepository.createApiCredentials(headers = headers)
        } else {
            derived
        }

        return credentials
            .onRight { credentialsStore.store(userWalletId = userWalletId, credentials = it) }
            .mapLeft { it.toOnboardingError() }
    }

    private companion object {
        const val CLOB_AUTH_NONCE = "0"
    }
}