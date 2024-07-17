package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for saving hash that failed to submit during staking confirmation
 */
class SaveUnsubmittedHashUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(transactionId: String, transactionHash: String): Either<StakingError, Unit> {
        return Either.catch {
            stakingRepository.storeUnsubmittedHash(
                unsubmittedTransactionMetadata = UnsubmittedTransactionMetadata(
                    transactionId = transactionId,
                    transactionHash = transactionHash,
                ),
            )
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }
}
