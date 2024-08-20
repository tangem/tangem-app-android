package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for staking gas estimation.
 */
class EstimateGasUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        params: ActionParams,
    ): Either<StakingError, StakingGasEstimate> {
        return Either.catch {
            stakingRepository.estimateGas(userWalletId, network, params)
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }
}
