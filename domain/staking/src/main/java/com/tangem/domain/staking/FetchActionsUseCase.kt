package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.repositories.StakingActionRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for getting pending actions list.
 */
class FetchActionsUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingActionRepository: StakingActionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        networkType: NetworkType,
        stakingActionStatus: StakingActionStatus,
    ): Either<StakingError, Unit> {
        return Either
            .catch {
                val actions = stakingRepository.getActions(
                    userWalletId = userWalletId,
                    cryptoCurrency = cryptoCurrency,
                    networkType = networkType,
                    stakingActionStatus = stakingActionStatus,
                )

                stakingActionRepository.store(userWalletId, cryptoCurrency.id, actions)
            }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}