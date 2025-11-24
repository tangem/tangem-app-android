package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakeKitRepository

/**
 * Use case for getting staking yield for staking scenario start.
 */
class GetYieldUseCase(
    private val stakeKitRepository: StakeKitRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Either<StakingError, Yield> {
        return Either
            .catch { stakeKitRepository.getYield(cryptoCurrencyId, symbol) }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }

    suspend operator fun invoke(yieldId: String): Either<StakingError, Yield> {
        return Either
            .catch { stakeKitRepository.getYield(yieldId) }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}