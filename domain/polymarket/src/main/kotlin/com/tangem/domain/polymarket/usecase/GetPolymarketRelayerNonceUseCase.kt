package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import java.math.BigInteger

/**
 * Reads the relayer nonce for [ownerAddress], the value the approvals batch is signed over.
 */
class GetPolymarketRelayerNonceUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(ownerAddress: String): Either<PolymarketOnboardingError, BigInteger> =
        polymarketRepository.getRelayerNonce(ownerAddress = ownerAddress).mapLeft { it.toOnboardingError() }
}