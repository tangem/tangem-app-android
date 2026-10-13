package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError

/**
 * Claim payload for the user's module, always taken from the network.
 *
 * A cached payload would carry a stale cumulative proof, so the repository never stores one and neither does any
 * caller: the sheet refetches whenever it reloads, and the claim refetches again right before signing.
 */
class GetFreshYieldClaimCalldataUseCase(
    private val yieldIncentivesRepository: YieldIncentivesRepository,
) {

    suspend operator fun invoke(chainId: Int, userAddress: String): Either<YieldIncentivesError, YieldClaimCalldata> =
        yieldIncentivesRepository.getClaimCalldata(chainId = chainId, userAddress = userAddress)
}