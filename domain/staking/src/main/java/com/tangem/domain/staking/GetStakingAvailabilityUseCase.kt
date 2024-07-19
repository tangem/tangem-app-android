package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Use case for getting info about staking capability in tangem app.
 */
class GetStakingAvailabilityUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): Either<StakingError, StakingAvailability> {
        return Either
            .catch { stakingRepository.getStakingAvailabilityForActions(cryptoCurrencyId, symbol) }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}