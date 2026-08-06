package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketOnboardingError

/**
 * Reports whether Polymarket trading is blocked for the caller's region. `true` means blocked.
 */
class CheckPolymarketGeoblockUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(): Either<PolymarketOnboardingError, Boolean> =
        polymarketRepository.checkGeoblock().mapLeft { it.toOnboardingError() }
}