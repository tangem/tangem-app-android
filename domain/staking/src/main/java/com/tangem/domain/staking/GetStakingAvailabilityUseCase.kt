package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for getting info about staking capability in tangem app.
 */
class GetStakingAvailabilityUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<StakingError, StakingAvailability> {
        return Either
            .catch {
                stakingRepository.getStakingAvailability(
                    userWalletId = userWalletId,
                    cryptoCurrency = cryptoCurrency,
                )
            }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}