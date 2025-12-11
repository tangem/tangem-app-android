package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakeKitRepository

/**
 * Use case for getting enabled tokens
 */
class FetchStakingTokensUseCase(
    private val stakeKitRepository: StakeKitRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {
    suspend operator fun invoke(): Either<StakingError, Unit> {
        return either {
            catch(
                block = { stakeKitRepository.fetchYields() },
                catch = { stakingErrorResolver.resolve(it) },
            )
        }
    }
}