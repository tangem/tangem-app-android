package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.NetworkType
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for getting pending actions list.
 */
class GetActionsUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        networkType: NetworkType,
    ): Either<StakingError, List<StakingAction>> {
        return Either
            .catch {
                stakingRepository.getActions(
                    userWalletId = userWalletId,
                    cryptoCurrency = cryptoCurrency,
                    networkType = networkType,
                )
            }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}
