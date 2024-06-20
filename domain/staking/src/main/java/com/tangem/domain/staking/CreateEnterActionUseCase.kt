package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.Token
import com.tangem.domain.staking.model.action.EnterAction
import com.tangem.domain.staking.model.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakingRepository
import kotlinx.coroutines.delay
import java.math.BigDecimal

/**
 * Use case for creating enter action
 */
class CreateEnterActionUseCase(private val stakingRepository: StakingRepository) {

    suspend operator fun invoke(
        integrationId: String,
        amount: BigDecimal,
        address: String,
        validatorAddress: String,
        token: Token,
    ): Either<Throwable, Pair<EnterAction, StakingTransaction>> {
        return Either.catch {
            val createAction = stakingRepository.createEnterAction(
                integrationId = integrationId,
                amount = amount,
                address = address,
                validatorAddress = validatorAddress,
                token = token,
            )

            // workaround, sometimes transaction is not created immediately after actions/enter
            delay(PATCH_TRANSACTION_REQUEST_DELAY)

            val createdTransaction = createAction.transactions?.get(0) ?: error("No available transaction to patch")
            val patchedTransaction = stakingRepository.constructTransaction(createdTransaction.id)

            createAction to patchedTransaction
        }
    }

    companion object {
        private const val PATCH_TRANSACTION_REQUEST_DELAY = 1000L
    }
}
