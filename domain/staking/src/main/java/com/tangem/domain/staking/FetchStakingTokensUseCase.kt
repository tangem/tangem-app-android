package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.staking.error.StakingTokensError
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for getting enabled tokens
 */
class FetchStakingTokensUseCase(
    private val stakingRepository: StakingRepository,
) {
    suspend operator fun invoke(): Either<Throwable, Unit> {
        return either {
            catch(
                block = { stakingRepository.fetchEnabledYields() },
                catch = { StakingTokensError.DataError(it) },
            )
        }
    }
}