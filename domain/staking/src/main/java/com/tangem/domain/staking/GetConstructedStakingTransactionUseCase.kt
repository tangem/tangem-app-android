package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver

class GetConstructedStakingTransactionUseCase(
    private val stakeKitRepository: StakeKitRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        networkId: Network.RawID,
        fee: Fee,
        amount: Amount,
        transactionId: String,
    ): Either<StakingError, Pair<StakingTransaction, TransactionData.Compiled>> = Either.catch {
        stakeKitRepository.constructTransaction(
            networkId = networkId,
            fee = fee,
            amount = amount,
            transactionId = transactionId,
        )
    }.mapLeft {
        stakingErrorResolver.resolve(it)
    }
}