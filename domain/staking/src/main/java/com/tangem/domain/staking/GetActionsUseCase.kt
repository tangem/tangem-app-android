package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.repositories.StakingActionRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting pending actions list.
 */
class GetActionsUseCase(
    private val stakingActionRepository: StakingActionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Either<StakingError, Flow<List<StakingAction>>> {
        return Either.catch {
            stakingActionRepository.get(
                userWalletId = userWalletId,
                cryptoCurrencyId = cryptoCurrencyId,
            )
        }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}
