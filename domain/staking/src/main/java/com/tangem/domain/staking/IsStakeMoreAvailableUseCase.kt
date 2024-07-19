package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.Network

class IsStakeMoreAvailableUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(networkId: Network.ID): Either<StakingError, Boolean> {
        return Either
            .catch { stakingRepository.isStakeMoreAvailable(networkId) }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}