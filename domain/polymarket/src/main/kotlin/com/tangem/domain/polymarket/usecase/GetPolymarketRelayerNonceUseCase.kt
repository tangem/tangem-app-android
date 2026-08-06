package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import java.math.BigInteger

/**
 * Reads the relayer nonce for the owner address in [addresses], the value the approvals batch is signed over.
 */
class GetPolymarketRelayerNonceUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(addresses: PolymarketAddresses): Either<PolymarketOnboardingError, BigInteger> =
        polymarketRepository.getRelayerNonce(ownerAddress = addresses.ownerAddress)
            .mapLeft { it.toOnboardingError() }
}