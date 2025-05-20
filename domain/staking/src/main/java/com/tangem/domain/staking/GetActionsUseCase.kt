package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.repositories.StakingActionRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Use case for getting pending actions list.
 */
class GetActionsUseCase(
    private val stakingActionRepository: StakingActionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): EitherFlow<StakingError, List<StakingAction>> {
        return stakingActionRepository.get(
            userWalletId = userWalletId,
            cryptoCurrencyId = cryptoCurrencyId,
        ).map<List<StakingAction>, Either<StakingError, List<StakingAction>>> { it.right() }
            .catch { emit(stakingErrorResolver.resolve(it).left()) }
    }
}