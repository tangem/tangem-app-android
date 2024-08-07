package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for getting enabled tokens
 */
class FetchStakingTokensUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {
    suspend operator fun invoke(isRefresh: Boolean = false): Either<StakingError, Unit> {
        return either {
            catch(
                block = { stakingRepository.fetchEnabledYields(isRefresh) },
                catch = { stakingErrorResolver.resolve(it) },
            )
        }
    }
}