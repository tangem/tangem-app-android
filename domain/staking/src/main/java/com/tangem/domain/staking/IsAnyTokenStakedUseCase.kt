package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.models.wallet.UserWalletId

class IsAnyTokenStakedUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<StakingError, Boolean> {
        return Either
            .catch { stakingRepository.isAnyTokenStaked(userWalletId) }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}