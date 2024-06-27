package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for getting entry info about staking on token screen.
 */
class GetStakingEntryInfoUseCase(private val stakingRepository: StakingRepository) {

    suspend operator fun invoke(integrationId: String): Either<Throwable, StakingEntryInfo> {
        return Either.catch { stakingRepository.getEntryInfo(integrationId) }
    }
}