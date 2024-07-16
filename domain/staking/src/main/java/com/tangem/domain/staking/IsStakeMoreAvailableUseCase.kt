package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.Network

class IsStakeMoreAvailableUseCase(
    private val stakingRepository: StakingRepository,
) {

    operator fun invoke(networkId: Network.ID): Either<Throwable, Boolean> {
        return Either.catch { stakingRepository.isStakeMoreAvailable(networkId) }
    }
}
