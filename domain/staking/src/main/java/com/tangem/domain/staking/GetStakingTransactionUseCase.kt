package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.delay

/**
 * Use case for creating enter action
 */
class GetStakingTransactionUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        params: ActionParams,
    ): Either<StakingError, List<StakingTransaction>> {
        return Either.catch {
            val createAction = stakingRepository.createAction(userWalletId, network, params)

            // workaround, sometimes transaction is not created immediately after actions/enter
            delay(PATCH_TRANSACTION_REQUEST_DELAY)

            createAction.transactions ?: error("No available transaction to patch")
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }

    companion object {
        private const val PATCH_TRANSACTION_REQUEST_DELAY = 1000L
    }
}