package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketGeoblock
import com.tangem.domain.polymarket.model.PolymarketOnboardingError

class CheckPolymarketGeoblockUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(): Either<PolymarketOnboardingError, PolymarketGeoblock> =
        polymarketRepository.checkGeoblock().mapLeft { it.toOnboardingError() }
}