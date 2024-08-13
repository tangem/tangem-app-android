package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository

class GetConstructedStakingTransactionUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        networkId: String,
        fee: Fee,
        transactionId: String,
    ): Either<StakingError, Pair<StakingTransaction, TransactionData.Compiled>> = Either.catch {
        stakingRepository.constructTransaction(networkId, fee, transactionId)
    }.mapLeft {
        stakingErrorResolver.resolve(it)
    }
}