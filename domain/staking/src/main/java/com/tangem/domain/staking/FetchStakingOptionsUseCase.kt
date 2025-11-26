package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakeKitRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Use case for fetching all staking options from all providers
 * Fetches both StakeKit yields and P2P vaults
 */
class FetchStakingOptionsUseCase(
    private val stakeKitRepository: StakeKitRepository,
    private val p2pRepository: P2PEthPoolRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {
    suspend operator fun invoke(): Either<StakingError, Unit> {
        return either {
            catch(
                block = {
                    coroutineScope {
                        launch { stakeKitRepository.fetchYields() }
                        launch { p2pRepository.fetchVaults() }
                    }
                },
                catch = { stakingErrorResolver.resolve(it) },
            )
        }
    }
}