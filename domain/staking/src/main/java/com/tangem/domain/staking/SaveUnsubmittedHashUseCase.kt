package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingTransactionHashRepository

/**
 * Use case for saving hash that failed to submit during staking confirmation
 */
class SaveUnsubmittedHashUseCase(
    private val stakingTransactionHashRepository: StakingTransactionHashRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(transactionId: String, transactionHash: String): Either<StakingError, Unit> {
        return Either.catch {
            stakingTransactionHashRepository.storeUnsubmittedHash(
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