package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketOnboardingError

/**
 * Refreshes the CLOB's cached collateral balance and allowance for [ownerAddress]'s deposit wallet, so that
 * the approvals granted on-chain become visible to the order book.
 */
class SyncBalanceAllowanceUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
    ): Either<PolymarketOnboardingError, Unit> = polymarketRepository
        .syncBalanceAllowance(ownerAddress = ownerAddress, credentials = credentials)
        .mapLeft { it.toOnboardingError() }
}